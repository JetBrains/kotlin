/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import junit.framework.TestCase
import org.jetbrains.kotlin.KtInMemoryTextSourceFile
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory0
import org.jetbrains.kotlin.checkers.diagnostics.factories.SyntaxErrorDiagnosticFactory
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.doFirResolveTestBench
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.createAllCompilerResolveProcessors
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinBaseTest
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.toSourceLinesMapping
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.Assert
import java.io.File
import java.util.regex.Pattern
import kotlin.reflect.jvm.javaField

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractFirOldFrontendLightClassesTest :
    KotlinMultiFileTestWithJava<AbstractFirOldFrontendLightClassesTest.TestModule, AbstractFirOldFrontendLightClassesTest.TestFile>() {
    protected lateinit var environment: KotlinCoreEnvironment

    protected val project: Project
        get() = environment.project

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        environment = createEnvironment(wholeFile, files)
        setupEnvironment(environment)
        analyzeAndCheck(wholeFile, files)
    }

    private fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        if (files.any { "FIR_IGNORE" in it.directives }) return
        try {
            analyzeAndCheckUnhandled(testDataFile, files, useLightTree)
        } catch (t: AssertionError) {
            throw t
        } catch (t: Throwable) {
            throw t
        }
    }

    private val useLightTree: Boolean
        get() = false

    private val useLazyBodiesModeForRawFir: Boolean
        get() = false

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        PsiElementFinder.EP.getPoint(environment.project).unregisterExtension(JavaElementFinder::class.java)
    }

    override fun tearDown() {
        this::environment.javaField!![this] = null
        super.tearDown()
    }

    override fun createTestModule(
        name: String,
        dependencies: List<String>,
        friends: List<String>,
    ): TestModule =
        TestModule(name, dependencies, friends)

    override fun createTestFile(module: TestModule?, fileName: String, text: String, directives: Directives): TestFile =
        TestFile(module, fileName, text, directives)

    private fun analyzeAndCheckUnhandled(testDataFile: File, files: List<TestFile>, useLightTree: Boolean = false) {
        val groupedByModule = files.groupBy(TestFile::module)

        val modules = createModules(groupedByModule)

        //For BuiltIns, registered in sessionProvider automatically
        val allProjectScope = GlobalSearchScope.allScope(project)

        val configToSession = modules.mapValues { (config, info) ->
            val moduleFiles = groupedByModule.getValue(config)
            val scope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(
                project,
                moduleFiles.mapNotNull { it.ktFile }
            )
            val projectEnvironment = environment.toVfsBasedProjectEnvironment()

            val configuration = CompilerConfiguration.create().apply {
                this.languageVersionSettings = config?.languageVersionSettings ?: LanguageVersionSettingsImpl.DEFAULT
            }
            FirSessionFactoryHelper.createSessionWithDependencies(
                Name.identifier(info.name.asString().removeSurrounding("<", ">")),
                info.platform,
                projectEnvironment,
                configuration,
                javaSourcesScope = PsiBasedProjectFileSearchScope(scope),
                librariesScope = PsiBasedProjectFileSearchScope(allProjectScope),
                incrementalCompilationContext = null,
                extensionRegistrars = emptyList(),
                needRegisterJavaElementFinder = true
            )
        }

        val firFilesPerSession = mutableMapOf<FirSession, List<FirFile>>()

        // TODO: make module/session/transformer handling like in AbstractFirMultiModuleTest (IDE)
        for ((testModule, testFilesInModule) in groupedByModule) {
            val ktFiles = getKtFiles(testFilesInModule, true)

            val session = configToSession.getValue(testModule)

            val firFiles = mutableListOf<FirFile>()
            mapKtFilesToFirFiles(session, ktFiles, firFiles, useLightTree)
            firFilesPerSession[session] = firFiles
        }

        runAnalysis(testDataFile, firFilesPerSession)
    }

    protected open fun getKtFiles(testFiles: List<TestFile>, includeExtras: Boolean): List<KtFile> {
        var declareFlexibleType = false
        var declareCheckType = false
        val ktFiles = arrayListOf<KtFile>()
        for (testFile in testFiles) {
            ktFiles.addIfNotNull(testFile.ktFile)
            declareFlexibleType = declareFlexibleType or testFile.declareFlexibleType
            declareCheckType = declareCheckType or testFile.declareCheckType
        }

        if (includeExtras) {
            if (declareFlexibleType) {
                ktFiles.add(
                    KtTestUtil.createFile(
                        "EXPLICIT_FLEXIBLE_TYPES.kt",
                        EXPLICIT_FLEXIBLE_TYPES_DECLARATIONS,
                        project
                    )
                )
            }
            if (declareCheckType) {
                val checkTypeDeclarations = File("$HELPERS_PATH/types/checkType.kt").readText()

                ktFiles.add(
                    KtTestUtil.createFile(
                        "CHECK_TYPE.kt",
                        checkTypeDeclarations,
                        project
                    )
                )
            }
        }

        return ktFiles
    }

    private fun mapKtFilesToFirFiles(session: FirSession, ktFiles: List<KtFile>, firFiles: MutableList<FirFile>, useLightTree: Boolean) {
        val firProvider = (session.firProvider as FirProviderImpl)
        if (useLightTree) {
            val lightTreeBuilder = LightTree2Fir(session, firProvider.kotlinScopeProvider)
            ktFiles.mapTo(firFiles) {
                val firFile =
                    lightTreeBuilder.buildFirFile(
                        it.text,
                        KtInMemoryTextSourceFile(it.name, it.virtualFilePath, it.text),
                        it.text.toSourceLinesMapping()
                    )
                (session.firProvider as FirProviderImpl).recordFile(firFile)
                firFile
            }
        } else {
            val firBuilder = PsiRawFirBuilder(
                session,
                firProvider.kotlinScopeProvider,
                bodyBuildingMode = BodyBuildingMode.lazyBodies(useLazyBodiesModeForRawFir)
            )
            ktFiles.mapTo(firFiles) {
                val firFile = firBuilder.buildFirFile(it)
                firProvider.recordFile(firFile)
                firFile
            }
        }
    }

    private fun runAnalysis(testDataFile: File, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        for ((session, firFiles) in firFilesPerSession) {
            doFirResolveTestBench(firFiles, createAllCompilerResolveProcessors(session), gc = false)
        }
        checkResultingFirFiles(testDataFile)
    }

    private fun checkResultingFirFiles(testDataFile: File) {
        val ourFinders = PsiElementFinder.EP.getPoint(project).extensions.filterIsInstance<FirJavaElementFinder>()

        assertNotEmpty(ourFinders)

        val stringBuilder = StringBuilder()

        for (qualifiedName in InTextDirectivesUtils.findListWithPrefixes(testDataFile.readText(), "// LIGHT_CLASS_FQ_NAME: ")) {
            val fqName = FqName(qualifiedName)
            val packageName = fqName.parent().asString()

            val ourFinder = ourFinders.firstOrNull { finder -> finder.findPackage(packageName) != null }
            assertNotNull("PsiPackage for ${fqName.parent()} was not found", ourFinder)
            ourFinder!!

            val psiPackage = ourFinder.findPackage(fqName.parent().asString())
            assertNotNull("PsiPackage for ${fqName.parent()} is null", psiPackage)

            val psiClass = assertInstanceOf(
                ourFinder.findClass(qualifiedName, GlobalSearchScope.allScope(project)),
                ClsClassImpl::class.java
            )

            psiClass.appendMirrorText(0, stringBuilder)
            stringBuilder.appendLine()
        }

        val expectedPath = testDataFile.path.replace(".kt", ".txt")
        TestDataAssertions.assertEqualsToFile(File(expectedPath), stringBuilder.toString())
    }

    override fun createTestFileFromPath(filePath: String): File {
        return File(filePath)
    }

    private fun createModules(groupedByModule: Map<TestModule?, List<TestFile>>): MutableMap<TestModule?, ModuleInfo> {
        val modules =
            HashMap<TestModule?, ModuleInfo>()

        for (testModule in groupedByModule.keys) {
            val module = if (testModule == null)
                createSealedModule()
            else
                createModule(testModule.name)

            modules[testModule] = module
        }

        for (testModule in groupedByModule.keys) {
            if (testModule == null) continue

            val module = modules[testModule]!!
            val dependencies = ArrayList<ModuleInfo>()
            dependencies.add(module)
            for (dependency in testModule.dependencies) {
                dependencies.add(modules[dependency as TestModule?]!!)
            }


            dependencies.add(builtInsModuleInfo)
            (module as TestModuleInfo).dependencies.addAll(dependencies)
        }

        return modules
    }

    private val builtInsModuleInfo = BuiltInModuleInfo(Name.special("<built-ins>"))

    private fun createModule(moduleName: String): TestModuleInfo {
        parseModulePlatformByName(moduleName)
        return TestModuleInfo(Name.special("<$moduleName>"))
    }

    private class BuiltInModuleInfo(override val name: Name) : ModuleInfo {
        override val platform: TargetPlatform
            get() = JvmPlatforms.unspecifiedJvmPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = JvmPlatformAnalyzerServices

        override fun dependencies(): List<ModuleInfo> {
            return listOf(this)
        }
    }

    private class TestModuleInfo(override val name: Name) : ModuleInfo {
        override val platform: TargetPlatform
            get() = JvmPlatforms.unspecifiedJvmPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = JvmPlatformAnalyzerServices

        val dependencies = mutableListOf<ModuleInfo>(this)
        override fun dependencies(): List<ModuleInfo> {
            return dependencies
        }
    }

    private fun createSealedModule(): TestModuleInfo {
        return createModule("test-module-jvm").apply {
            dependencies += builtInsModuleInfo
        }
    }

    inner class TestFile(
        val module: TestModule?,
        val fileName: String,
        textWithMarkers: String,
        directives: Directives,
    ) : KotlinBaseTest.TestFile(fileName, textWithMarkers, directives) {
        val diagnosedRanges: MutableList<DiagnosedRange> = mutableListOf()
        val diagnosedRangesToDiagnosticNames: MutableMap<IntRange, MutableSet<String>> = mutableMapOf()
        val expectedText: String
        val clearText: String
        private val createKtFile: Lazy<KtFile?>
        private val whatDiagnosticsToConsider: Condition<Diagnostic>
        val customLanguageVersionSettings: LanguageVersionSettings?
        val jvmTarget: JvmTarget?
        val declareCheckType: Boolean = CHECK_TYPE_DIRECTIVE in directives
        val declareFlexibleType: Boolean
        private val markDynamicCalls: Boolean
        val withNewInferenceDirective: Boolean
        val newInferenceEnabled: Boolean
        val renderDiagnosticMessages: Boolean
        val renderDiagnosticsFullText: Boolean

        init {
            this.whatDiagnosticsToConsider = parseDiagnosticFilterDirective(directives, declareCheckType)
            this.customLanguageVersionSettings = parseLanguageVersionSettings(directives)
            this.jvmTarget = parseJvmTarget(directives)
            this.declareFlexibleType = EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE in directives
            this.markDynamicCalls = MARK_DYNAMIC_CALLS_DIRECTIVE in directives
            this.withNewInferenceDirective = WITH_NEW_INFERENCE_DIRECTIVE in directives
            this.newInferenceEnabled =
                customLanguageVersionSettings?.supportsFeature(LanguageFeature.NewInference) ?: shouldUseNewInferenceForTests()
            if (fileName.endsWith(".java")) {
                // TODO: check there are no syntax errors in .java sources
                this.createKtFile = lazyOf(null)
                this.clearText = textWithMarkers
                this.expectedText = this.clearText
            } else {
                this.expectedText = textWithMarkers
                this.clearText =
                    CheckerTestUtil.parseDiagnosedRanges(addExtras(expectedText), diagnosedRanges, diagnosedRangesToDiagnosticNames)
                this.createKtFile = lazy { createCheckAndReturnPsiFile(fileName, clearText, project) }
            }
            this.renderDiagnosticMessages = RENDER_DIAGNOSTIC_ARGUMENTS in directives
            this.renderDiagnosticsFullText = RENDER_DIAGNOSTICS_FULL_TEXT in directives
        }

        val ktFile: KtFile? by createKtFile

        private val imports: String
            get() = buildString {
                // Line separator is "\n" intentionally here (see DocumentImpl.assertValidSeparators)
                if (declareFlexibleType) {
                    append(EXPLICIT_FLEXIBLE_TYPES_IMPORT + "\n")
                }
            }

        private val extras: String
            get() = "/*extras*/\n$imports/*extras*/\n\n"

        fun addExtras(text: String): String =
            addImports(text, extras)


        private fun addImports(text: String, imports: String): String {
            var result = text
            val pattern = Pattern.compile("^package [.\\w\\d]*\n", Pattern.MULTILINE)
            val matcher = pattern.matcher(result)
            result = if (matcher.find()) {
                // add imports after the package directive
                result.substring(0, matcher.end()) + imports + result.substring(matcher.end())
            } else {
                // add imports at the beginning
                imports + result
            }
            return result
        }

        private fun shouldUseNewInferenceForTests(): Boolean {
            if (System.getProperty("kotlin.ni") == "true") return true
            return LanguageVersionSettingsImpl.DEFAULT.supportsFeature(LanguageFeature.NewInference)
        }

        override fun toString(): String = ktFile?.name ?: "Java file"
    }

    class TestModule(name: String, dependencies: List<String>, friends: List<String>) :
        KotlinBaseTest.TestModule(name, dependencies, friends) {
        lateinit var languageVersionSettings: LanguageVersionSettings
    }

    protected fun parseModulePlatformByName(moduleName: String): TargetPlatform? {
        val nameSuffix = moduleName.substringAfterLast("-", "").uppercase()
        return when {
            nameSuffix == "COMMON" -> CommonPlatforms.defaultCommonPlatform
            nameSuffix == "JVM" -> JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): determine JvmTarget precisely
            nameSuffix == "JS" -> JsPlatforms.defaultJsPlatform
            nameSuffix.isEmpty() -> null // TODO(dsavvinov): this leads to 'null'-platform in ModuleDescriptor
            else -> throw IllegalStateException("Can't determine platform by name $nameSuffix")
        }
    }

    companion object {
        private const val HELPERS_PATH = "./compiler/testData/diagnostics/helpers"
        private val DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS"
        private val DIAGNOSTICS_PATTERN: Pattern = Pattern.compile("([+\\-!])(\\w+)\\s*")
        private val DIAGNOSTICS_TO_INCLUDE_ANYWAY: Set<DiagnosticFactory<*>> = setOf(
            Errors.UNRESOLVED_REFERENCE,
            Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
            SyntaxErrorDiagnosticFactory.INSTANCE,
            DebugInfoDiagnosticFactory0.ELEMENT_WITH_ERROR_TYPE,
            DebugInfoDiagnosticFactory0.MISSING_UNRESOLVED,
            DebugInfoDiagnosticFactory0.UNRESOLVED_WITH_TARGET
        )

        private val CHECK_TYPE_DIRECTIVE = "CHECK_TYPE"

        private val EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE = "EXPLICIT_FLEXIBLE_TYPES"
        private val EXPLICIT_FLEXIBLE_PACKAGE = InternalFlexibleTypeTransformer.FLEXIBLE_TYPE_CLASSIFIER.packageFqName.asString()
        private val EXPLICIT_FLEXIBLE_CLASS_NAME = InternalFlexibleTypeTransformer.FLEXIBLE_TYPE_CLASSIFIER.relativeClassName.asString()
        private val EXPLICIT_FLEXIBLE_TYPES_DECLARATIONS = "\npackage " + EXPLICIT_FLEXIBLE_PACKAGE +
                "\npublic class " + EXPLICIT_FLEXIBLE_CLASS_NAME + "<L, U>"
        private val EXPLICIT_FLEXIBLE_TYPES_IMPORT = "import $EXPLICIT_FLEXIBLE_PACKAGE.$EXPLICIT_FLEXIBLE_CLASS_NAME"
        private val MARK_DYNAMIC_CALLS_DIRECTIVE = "MARK_DYNAMIC_CALLS"
        private val WITH_NEW_INFERENCE_DIRECTIVE = "WITH_NEW_INFERENCE"
        private val RENDER_DIAGNOSTIC_ARGUMENTS = "RENDER_DIAGNOSTIC_ARGUMENTS"
        private val RENDER_DIAGNOSTICS_FULL_TEXT = "RENDER_DIAGNOSTICS_FULL_TEXT"


        fun parseDiagnosticFilterDirective(
            directiveMap: Directives,
            allowUnderscoreUsage: Boolean,
        ): Condition<Diagnostic> {
            val directives = directiveMap[DIAGNOSTICS_DIRECTIVE]
            val initialCondition =
                if (allowUnderscoreUsage)
                    Condition<Diagnostic> { it.factory.name != "UNDERSCORE_USAGE_WITHOUT_BACKTICKS" }
                else
                    Conditions.alwaysTrue()

            if (directives == null) {
                // If "!API_VERSION" is present, disable the NEWER_VERSION_IN_SINCE_KOTLIN diagnostic.
                // Otherwise it would be reported in any non-trivial test on the @SinceKotlin value.
                if (API_VERSION_DIRECTIVE in directiveMap) {
                    return Conditions.and(initialCondition, Condition { diagnostic ->
                        diagnostic.factory !== Errors.NEWER_VERSION_IN_SINCE_KOTLIN
                    })
                }
                return initialCondition
            }

            var condition = initialCondition
            val matcher = DIAGNOSTICS_PATTERN.matcher(directives)
            if (!matcher.find()) {
                Assert.fail(
                    "Wrong syntax in the '// $DIAGNOSTICS_DIRECTIVE: ...' directive:\n" +
                            "found: '$directives'\n" +
                            "Must be '([+-!]DIAGNOSTIC_FACTORY_NAME|ERROR|WARNING|INFO)+'\n" +
                            "where '+' means 'include'\n" +
                            "      '-' means 'exclude'\n" +
                            "      '!' means 'exclude everything but this'\n" +
                            "directives are applied in the order of appearance, i.e. !FOO +BAR means include only FOO and BAR"
                )
            }

            var first = true
            do {
                val operation = matcher.group(1)
                val name = matcher.group(2)

                val newCondition: Condition<Diagnostic> =
                    if (name in setOf("ERROR", "WARNING", "INFO")) {
                        Condition { diagnostic -> diagnostic.severity == Severity.valueOf(name) }
                    } else {
                        Condition { diagnostic -> name == diagnostic.factory.name }
                    }

                when (operation) {
                    "!" -> {
                        if (!first) {
                            Assert.fail(
                                "'$operation$name' appears in a position rather than the first one, " +
                                        "which effectively cancels all the previous filters in this directive"
                            )
                        }
                        condition = newCondition
                    }

                    "+" -> condition = Conditions.or(condition, newCondition)
                    "-" -> condition = Conditions.and(condition, Conditions.not(newCondition))
                }
                first = false
            } while (matcher.find())

            // We always include UNRESOLVED_REFERENCE and SYNTAX_ERROR because they are too likely to indicate erroneous test data
            return Conditions.or(
                condition,
                Condition { diagnostic -> diagnostic.factory in DIAGNOSTICS_TO_INCLUDE_ANYWAY }
            )
        }

        private fun createCheckAndReturnPsiFile(fileName: String, text: String, project: Project): KtFile {
            val myFile = KtTestUtil.createFile(fileName, text, project)
            // ensure parsed
            myFile.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    element.acceptChildren(this)
                }
            })
            TestCase.assertEquals(
                "light virtual file text mismatch",
                text,
                (myFile.getVirtualFile() as LightVirtualFile).getContent().toString()
            )
            assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(myFile.getVirtualFile()))
            TestCase.assertEquals("doc text mismatch", text, myFile.getViewProvider().getDocument().getText())
            TestCase.assertEquals("psi text mismatch", text, myFile.getText())
            return myFile
        }
    }

    private fun parseJvmTarget(directiveMap: Directives) = directiveMap["JVM_TARGET"]?.let { JvmTarget.fromString(it) }
}
