/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.checkers

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.*
import org.junit.Assert

import java.io.File
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class BaseDiagnosticsTest : KotlinMultiFileTestWithJava<BaseDiagnosticsTest.TestModule, BaseDiagnosticsTest.TestFile>() {

    override fun createTestModule(name: String): TestModule {
        return TestModule(name)
    }

    override fun createTestFile(module: TestModule, fileName: String, text: String, directives: Map<String, String>): TestFile {
        return TestFile(module, fileName, text, directives)
    }

    override fun doMultiFileTest(file: File, modules: Map<String, KotlinMultiFileTestWithJava<BaseDiagnosticsTest.TestFile, BaseDiagnosticsTest.TestModule>.ModuleAndDependencies>, testFiles: List<TestFile>) {
        for (moduleAndDependencies in modules.values) {
            val dependencies = moduleAndDependencies.dependencies.map(
                    { name ->
                        val dependency = modules[name] ?: error("Dependency not found: " +
                                                                name +
                                                                " for module " +
                                                                moduleAndDependencies.module.name)
                        dependency.module
                    }
            )
            moduleAndDependencies.module.getDependencies().addAll(dependencies)
        }

        analyzeAndCheck(file, testFiles)
    }

    protected abstract fun analyzeAndCheck(
            testDataFile: File,
            files: List<TestFile>
    )

    protected fun getJetFiles(testFiles: List<TestFile>, includeExtras: Boolean): List<KtFile> {
        var declareFlexibleType = false
        var declareCheckType = false
        val jetFiles = Lists.newArrayList<KtFile>()
        for (testFile in testFiles) {
            if (testFile.jetFile != null) {
                jetFiles.add(testFile.jetFile)
            }
            declareFlexibleType = declareFlexibleType or testFile.declareFlexibleType
            declareCheckType = declareCheckType or testFile.declareCheckType
        }

        if (includeExtras) {
            if (declareFlexibleType) {
                jetFiles.add(KotlinTestUtils.createFile("EXPLICIT_FLEXIBLE_TYPES.kt", EXPLICIT_FLEXIBLE_TYPES_DECLARATIONS, project))
            }
            if (declareCheckType) {
                jetFiles.add(KotlinTestUtils.createFile("CHECK_TYPE.kt", CHECK_TYPE_DECLARATIONS, project))
            }
        }

        return jetFiles
    }

    class TestModule(val name: String) : Comparable<TestModule> {
        private val dependencies = ArrayList<TestModule>()

        fun getDependencies(): MutableList<TestModule> {
            return dependencies
        }

        override fun compareTo(module: TestModule): Int {
            return name.compareTo(module.name)
        }

        override fun toString(): String {
            return name
        }
    }

    class DiagnosticTestLanguageVersionSettings(
            private val languageFeatures: Map<LanguageFeature, Boolean>, override val apiVersion: ApiVersion
    ) : LanguageVersionSettings {

        override fun supportsFeature(feature: LanguageFeature): Boolean {
            val enabled = languageFeatures[feature]
            return enabled ?: LanguageVersionSettingsImpl.DEFAULT.supportsFeature(feature)
        }

        override // TODO provide base language version
        val languageVersion: LanguageVersion
            get() = throw UnsupportedOperationException("This instance of LanguageVersionSettings should be used for tests only")

        override fun equals(obj: Any?): Boolean {
            return obj is DiagnosticTestLanguageVersionSettings &&
                   obj.languageFeatures == languageFeatures &&
                   obj.apiVersion == apiVersion
        }
    }

    inner class TestFile(
            val module: TestModule?,
            fileName: String,
            textWithMarkers: String,
            directives: Map<String, String>
    ) {
        private val diagnosedRanges = Lists.newArrayList<CheckerTestUtil.DiagnosedRange>()
        val expectedText: String
        private val clearText: String
        val jetFile: KtFile?
        private val whatDiagnosticsToConsider: Condition<Diagnostic>
        val customLanguageVersionSettings: LanguageVersionSettings
        private val declareCheckType: Boolean
        private val declareFlexibleType: Boolean
        val checkLazyLog: Boolean
        private val markDynamicCalls: Boolean
        val dynamicCallDescriptors: List<DeclarationDescriptor> = ArrayList()

        init {
            this.whatDiagnosticsToConsider = parseDiagnosticFilterDirective(directives)
            this.customLanguageVersionSettings = parseLanguageVersionSettings(directives)
            this.checkLazyLog = directives.containsKey(CHECK_LAZY_LOG_DIRECTIVE) || CHECK_LAZY_LOG_DEFAULT
            this.declareCheckType = directives.containsKey(CHECK_TYPE_DIRECTIVE)
            this.declareFlexibleType = directives.containsKey(EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE)
            this.markDynamicCalls = directives.containsKey(MARK_DYNAMIC_CALLS_DIRECTIVE)
            if (fileName.endsWith(".java")) {
                PsiFileFactory.getInstance(project).createFileFromText(fileName, JavaLanguage.INSTANCE, textWithMarkers)
                // TODO: check there's not syntax errors
                this.jetFile = null
                this.clearText = textWithMarkers
                this.expectedText = this.clearText
            }
            else {
                this.expectedText = textWithMarkers
                val textWithExtras = addExtras(expectedText)
                this.clearText = CheckerTestUtil.parseDiagnosedRanges(textWithExtras, diagnosedRanges)
                this.jetFile = TestCheckerUtil.createCheckAndReturnPsiFile(fileName, clearText, project)
                for (diagnosedRange in diagnosedRanges) {
                    diagnosedRange.file = jetFile
                }
            }
        }

        private val imports: String
            get() {
                var imports = ""
                if (declareCheckType) {
                    imports += CHECK_TYPE_IMPORT + "\n"
                }
                if (declareFlexibleType) {
                    imports += EXPLICIT_FLEXIBLE_TYPES_IMPORT + "\n"
                }
                return imports
            }

        private val extras: String
            get() = "/*extras*/\n$imports/*extras*/\n\n"

        private fun addExtras(text: String): String {
            return addImports(text, extras)
        }

        private fun stripExtras(actualText: StringBuilder) {
            val extras = extras
            val start = actualText.indexOf(extras)
            if (start >= 0) {
                actualText.delete(start, start + extras.length)
            }
        }

        private fun addImports(text: String, imports: String): String {
            var text = text
            val pattern = Pattern.compile("^package [\\.\\w\\d]*\n", Pattern.MULTILINE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                // add imports after the package directive
                text = text.substring(0, matcher.end()) + imports + text.substring(matcher.end())
            }
            else {
                // add imports at the beginning
                text = imports + text
            }
            return text
        }

        fun getActualText(bindingContext: BindingContext, actualText: StringBuilder, skipJvmSignatureDiagnostics: Boolean): Boolean {
            if (this.jetFile == null) {
                // TODO: check java files too
                actualText.append(this.clearText)
                return true
            }

            val jvmSignatureDiagnostics = if (skipJvmSignatureDiagnostics)
                emptySet<Diagnostic>()
            else
                computeJvmSignatureDiagnostics(bindingContext)

            val ok = booleanArrayOf(true)
            val diagnostics = ContainerUtil.filter(
                    CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile, markDynamicCalls, dynamicCallDescriptors).plus(
                            jvmSignatureDiagnostics),
                    whatDiagnosticsToConsider
            )

            val diagnosticToExpectedDiagnostic = ContainerUtil.newHashMap<Diagnostic, CheckerTestUtil.TextDiagnostic>()
            CheckerTestUtil.diagnosticsDiff(diagnosticToExpectedDiagnostic, diagnosedRanges, diagnostics, object : CheckerTestUtil.DiagnosticDiffCallbacks {

                override fun missingDiagnostic(diagnostic: CheckerTestUtil.TextDiagnostic, expectedStart: Int, expectedEnd: Int) {
                    val message = "Missing " + diagnostic.name + DiagnosticUtils.atLocation(jetFile, TextRange(expectedStart, expectedEnd))
                    System.err.println(message)
                    ok[0] = false
                }

                override fun wrongParametersDiagnostic(
                        expectedDiagnostic: CheckerTestUtil.TextDiagnostic,
                        actualDiagnostic: CheckerTestUtil.TextDiagnostic,
                        start: Int,
                        end: Int
                ) {
                    val message = "Parameters of diagnostic not equal at position "
                    +DiagnosticUtils.atLocation(jetFile, TextRange(start, end))
                    +". Expected: " + expectedDiagnostic.asString() + ", actual: " + actualDiagnostic.asString()
                    System.err.println(message)
                    ok[0] = false
                }

                override fun unexpectedDiagnostic(diagnostic: CheckerTestUtil.TextDiagnostic, actualStart: Int, actualEnd: Int) {
                    val message = "Unexpected " + diagnostic.name + DiagnosticUtils.atLocation(jetFile, TextRange(actualStart, actualEnd))
                    System.err.println(message)
                    ok[0] = false
                }
            })

            actualText.append(CheckerTestUtil.addDiagnosticMarkersToText(jetFile, diagnostics, diagnosticToExpectedDiagnostic, Function<PsiFile, String> { file -> file.text }))

            stripExtras(actualText)

            return ok[0]
        }

        private fun computeJvmSignatureDiagnostics(bindingContext: BindingContext): Set<Diagnostic> {
            val jvmSignatureDiagnostics = HashSet<Diagnostic>()
            val declarations = PsiTreeUtil.findChildrenOfType(jetFile, KtDeclaration::class.java)
            for (declaration in declarations) {
                val diagnostics = getJvmSignatureDiagnostics(declaration, bindingContext.diagnostics,
                                                             GlobalSearchScope.allScope(project)) ?: continue
                jvmSignatureDiagnostics.addAll(diagnostics.forElement(declaration))
            }
            return jvmSignatureDiagnostics
        }

        override fun toString(): String {
            return jetFile!!.name
        }
    }

    companion object {

        val DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS"
        val DIAGNOSTICS_PATTERN = Pattern.compile("([\\+\\-!])(\\w+)\\s*")
        val DIAGNOSTICS_TO_INCLUDE_ANYWAY: ImmutableSet<DiagnosticFactory<*>> = ImmutableSet.of(
                Errors.UNRESOLVED_REFERENCE,
                Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                CheckerTestUtil.SyntaxErrorDiagnosticFactory.INSTANCE,
                CheckerTestUtil.DebugInfoDiagnosticFactory.ELEMENT_WITH_ERROR_TYPE,
                CheckerTestUtil.DebugInfoDiagnosticFactory.MISSING_UNRESOLVED,
                CheckerTestUtil.DebugInfoDiagnosticFactory.UNRESOLVED_WITH_TARGET
        )

        val LANGUAGE_DIRECTIVE = "LANGUAGE"
        private val LANGUAGE_PATTERN = Pattern.compile("([\\+\\-])(\\w+)\\s*")

        val API_VERSION_DIRECTIVE = "API_VERSION"

        val CHECK_TYPE_DIRECTIVE = "CHECK_TYPE"
        val CHECK_TYPE_PACKAGE = "tests._checkType"
        private val CHECK_TYPE_DECLARATIONS = "\npackage " + CHECK_TYPE_PACKAGE +
                                              "\nfun <T> checkSubtype(t: T) = t" +
                                              "\nclass Inv<T>" +
                                              "\nfun <E> Inv<E>._() {}" +
                                              "\ninfix fun <T> T.checkType(f: Inv<T>.() -> Unit) {}"
        val CHECK_TYPE_IMPORT = "import $CHECK_TYPE_PACKAGE.*"

        val EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE = "EXPLICIT_FLEXIBLE_TYPES"
        val EXPLICIT_FLEXIBLE_PACKAGE = InternalFlexibleTypeTransformer.FLEXIBLE_TYPE_CLASSIFIER.packageFqName.asString()
        val EXPLICIT_FLEXIBLE_CLASS_NAME = InternalFlexibleTypeTransformer.FLEXIBLE_TYPE_CLASSIFIER.relativeClassName.asString()
        private val EXPLICIT_FLEXIBLE_TYPES_DECLARATIONS = "\npackage " + EXPLICIT_FLEXIBLE_PACKAGE +
                                                           "\npublic class " + EXPLICIT_FLEXIBLE_CLASS_NAME + "<L, U>"
        private val EXPLICIT_FLEXIBLE_TYPES_IMPORT = "import $EXPLICIT_FLEXIBLE_PACKAGE.$EXPLICIT_FLEXIBLE_CLASS_NAME"
        val CHECK_LAZY_LOG_DIRECTIVE = "CHECK_LAZY_LOG"
        val CHECK_LAZY_LOG_DEFAULT = "true" == System.getProperty("check.lazy.logs", "false")

        val MARK_DYNAMIC_CALLS_DIRECTIVE = "MARK_DYNAMIC_CALLS"

        private fun parseLanguageVersionSettings(directiveMap: Map<String, String>): LanguageVersionSettings? {
            val apiVersionString = directiveMap[API_VERSION_DIRECTIVE]
            val directives = directiveMap[LANGUAGE_DIRECTIVE]
            if (apiVersionString == null && directives == null) return null

            val apiVersion = (if (apiVersionString != null) ApiVersion.parse(apiVersionString) else ApiVersion.LATEST) ?: error("Unknown API version: " + apiVersionString!!)

            val languageFeatures = if (directives == null) emptyMap<LanguageFeature, Boolean>() else collectLanguageFeatureMap(directives)

            return DiagnosticTestLanguageVersionSettings(languageFeatures, apiVersion)
        }

        private fun collectLanguageFeatureMap(directives: String): Map<LanguageFeature, Boolean> {
            val matcher = LANGUAGE_PATTERN.matcher(directives)
            if (!matcher.find()) {
                Assert.fail(
                        "Wrong syntax in the '// !" + LANGUAGE_DIRECTIVE + ": ...' directive:\n" +
                        "found: '" + directives + "'\n" +
                        "Must be '([+-]LanguageFeatureName)+'\n" +
                        "where '+' means 'enable' and '-' means 'disable'\n" +
                        "and language feature names are names of enum entries in LanguageFeature enum class"
                )
            }

            val values = HashMap<LanguageFeature, Boolean>()
            do {
                val enable = matcher.group(1) == "+"
                val name = matcher.group(2)
                val feature = LanguageFeature.fromString(name)
                if (feature == null) {
                    Assert.fail(
                            "Language feature not found, please check spelling: " + name + "\n" +
                            "Known features:\n    " + join(Arrays.asList(*LanguageFeature.values()), "\n    ")
                    )
                }
                if (values.put(feature, enable) != null) {
                    Assert.fail("Duplicate entry for the language feature: " + name)
                }
            }
            while (matcher.find())

            return values
        }

        private fun parseDiagnosticFilterDirective(directiveMap: Map<String, String>): Condition<Diagnostic> {
            val directives = directiveMap[DIAGNOSTICS_DIRECTIVE]
            if (directives == null) {
                // If "!API_VERSION" is present, disable the NEWER_VERSION_IN_SINCE_KOTLIN diagnostic.
                // Otherwise it would be reported in any non-trivial test on the @SinceKotlin value.
                if (directiveMap.containsKey(API_VERSION_DIRECTIVE)) {
                    return Condition { diagnostic -> diagnostic.factory !== Errors.NEWER_VERSION_IN_SINCE_KOTLIN }
                }
                return Conditions.alwaysTrue<Diagnostic>()
            }
            var condition = Conditions.alwaysTrue<Diagnostic>()
            val matcher = DIAGNOSTICS_PATTERN.matcher(directives)
            if (!matcher.find()) {
                Assert.fail("Wrong syntax in the '// !" + DIAGNOSTICS_DIRECTIVE + ": ...' directive:\n" +
                            "found: '" + directives + "'\n" +
                            "Must be '([+-!]DIAGNOSTIC_FACTORY_NAME|ERROR|WARNING|INFO)+'\n" +
                            "where '+' means 'include'\n" +
                            "      '-' means 'exclude'\n" +
                            "      '!' means 'exclude everything but this'\n" +
                            "directives are applied in the order of appearance, i.e. !FOO +BAR means include only FOO and BAR")
            }
            var first = true
            do {
                val operation = matcher.group(1)
                val name = matcher.group(2)

                var newCondition: Condition<Diagnostic>
                if (ImmutableSet.of("ERROR", "WARNING", "INFO").contains(name)) {
                    val severity = Severity.valueOf(name)
                    newCondition = Condition<Diagnostic> { diagnostic -> diagnostic.severity == severity }
                }
                else {
                    newCondition = Condition<Diagnostic> { diagnostic -> name == diagnostic.factory.name }
                }
                if ("!" == operation) {
                    if (!first) {
                        Assert.fail("'" + operation + name + "' appears in a position rather than the first one, " +
                                    "which effectively cancels all the previous filters in this directive")
                    }
                    condition = newCondition
                }
                else if ("+" == operation) {
                    condition = Conditions.or(condition, newCondition)
                }
                else if ("-" == operation) {
                    condition = Conditions.and(condition, Conditions.not(newCondition))
                }
                first = false
            }
            while (matcher.find())
            // We always include UNRESOLVED_REFERENCE and SYNTAX_ERROR because they are too likely to indicate erroneous test data
            return Conditions.or(
                    condition,
                    Condition<Diagnostic> { diagnostic -> DIAGNOSTICS_TO_INCLUDE_ANYWAY.contains(diagnostic.factory) })
        }
    }
}
