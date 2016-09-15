/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.findUsages

import com.intellij.codeInsight.JavaTargetElementEvaluator
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.find.FindManager
import com.intellij.find.findUsages.*
import com.intellij.find.impl.FindManagerImpl
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.usageView.UsageInfo
import com.intellij.usages.TextChunk
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageViewPresentation
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider
import com.intellij.usages.rules.UsageFilteringRule
import com.intellij.usages.rules.UsageGroupingRule
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

abstract class AbstractFindUsagesTest : KotlinLightCodeInsightFixtureTestCase() {

    protected enum class OptionsParser {
        CLASS {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return KotlinClassFindUsagesOptions(project).apply {
                    isUsages = false
                    isSearchForTextOccurrences = false
                    searchConstructorUsages = false
                    for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                        if (parseCommonOptions(this, s)) continue

                        when (s) {
                            "constructorUsages" -> searchConstructorUsages = true
                            "derivedInterfaces" -> isDerivedInterfaces = true
                            "derivedClasses" -> isDerivedClasses = true
                            "functionUsages" -> isMethodsUsages = true
                            "propertyUsages" -> isFieldsUsages = true
                            else -> fail("Invalid option: " + s)
                        }
                    }
                }
            }
        },
        FUNCTION {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return KotlinFunctionFindUsagesOptions(project).apply {
                    isUsages = false
                    for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                        if (parseCommonOptions(this, s)) continue

                        when (s) {
                            "overrides" -> {
                                isOverridingMethods = true
                                isImplementingMethods = true
                            }
                            "overloadUsages" -> {
                                isIncludeOverloadUsages = true
                                isUsages = true
                            }
                            else -> fail("Invalid option: " + s)
                        }
                    }
                }
            }
        },
        PROPERTY {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return KotlinPropertyFindUsagesOptions(project).apply {
                    isUsages = false
                    for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                        if (parseCommonOptions(this, s)) continue

                        when (s) {
                            "overrides" -> searchOverrides = true
                            "skipRead" -> isReadAccess = false
                            "skipWrite" -> isWriteAccess = false
                            else -> fail("Invalid option: " + s)
                        }
                    }
                }
            }
        },
        JAVA_CLASS {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return KotlinClassFindUsagesOptions(project).apply {
                    isUsages = false
                    searchConstructorUsages = false
                    for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                        if (parseCommonOptions(this, s)) continue

                        when (s) {
                            "derivedInterfaces" -> isDerivedInterfaces = true
                            "derivedClasses" -> isDerivedClasses = true
                            "implementingClasses" -> isImplementingClasses = true
                            "methodUsages" -> isMethodsUsages = true
                            "fieldUsages" -> isFieldsUsages = true
                            else -> fail("Invalid option: " + s)
                        }
                    }
                }
            }
        },
        JAVA_METHOD {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return JavaMethodFindUsagesOptions(project).apply {
                    isUsages = false
                    for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                        if (parseCommonOptions(this, s)) continue

                        when (s) {
                            "overrides" -> {
                                isOverridingMethods = true
                                isImplementingMethods = true
                            }
                            else -> fail("Invalid option: " + s)
                        }
                    }
                }
            }
        },
        JAVA_FIELD {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return JavaVariableFindUsagesOptions(project)
            }
        },
        JAVA_PACKAGE {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return JavaPackageFindUsagesOptions(project)
            }
        },
        DEFAULT {
            override fun parse(text: String, project: Project): FindUsagesOptions {
                return FindUsagesOptions(project)
            }
        };

        abstract fun parse(text: String, project: Project): FindUsagesOptions

        companion object {

            protected fun parseCommonOptions(options: JavaFindUsagesOptions, s: String): Boolean {
                when (s) {
                    "usages" -> {
                        options.isUsages = true
                        return true
                    }
                    "skipImports" -> {
                        options.isSkipImportStatements = true
                        return true
                    }
                    "textOccurrences" -> {
                        options.isSearchForTextOccurrences = true
                        return true
                    }
                    else -> return false
                }

            }

            fun getParserByPsiElementClass(klass: Class<out PsiElement>): OptionsParser? {
                return when (klass) {
                    KtNamedFunction::class.java -> FUNCTION
                    KtProperty::class.java, KtParameter::class.java -> PROPERTY
                    KtClass::class.java -> CLASS
                    PsiMethod::class.java -> JAVA_METHOD
                    PsiClass::class.java -> JAVA_CLASS
                    PsiField::class.java -> JAVA_FIELD
                    PsiPackage::class.java -> JAVA_PACKAGE
                    KtTypeParameter::class.java -> DEFAULT
                    else -> null
                }

            }
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    public override fun setUp() {
        super.setUp()
        myFixture.testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/findUsages"
    }

    // used in Spring tests (outside main project!)
    protected open fun extraConfig(path: String) {
    }

    protected fun <T : PsiElement> doTest(path: String) {
        val mainFile = File(path)
        val mainFileName = mainFile.name
        val mainFileText = FileUtil.loadFile(mainFile, true)
        val prefix = mainFileName.substringBefore(".") + "."

        val isPropertiesFile = FileUtilRt.getExtension(path) == "properties"

        @Suppress("UNCHECKED_CAST")
        val caretElementClass = (if (!isPropertiesFile) {
            val caretElementClassNames = InTextDirectivesUtils.findLinesWithPrefixesRemoved(mainFileText, "// PSI_ELEMENT: ")
            Class.forName(caretElementClassNames.single())
        }
        else if (InTextDirectivesUtils.isDirectiveDefined(mainFileText, "## FIND_FILE_USAGES")) {
            PropertiesFile::class.java
        }
        else {
            Property::class.java
        }) as Class<T>

        val fixtureClasses = InTextDirectivesUtils.findListWithPrefixes(mainFileText, "// FIXTURE_CLASS: ")
        for (fixtureClass in fixtureClasses) {
            TestFixtureExtension.loadFixture(fixtureClass, myFixture.module)
        }

        try {
            extraConfig(path)

            val parser = OptionsParser.getParserByPsiElementClass(caretElementClass)

            val rootPath = path.substringBeforeLast("/") + "/"

            val rootDir = File(rootPath)
            val extraFiles = rootDir.listFiles { dir, name ->
                if (!name.startsWith(prefix) || name == mainFileName) return@listFiles false

                val ext = FileUtilRt.getExtension(name)
                ext == "kt"
                || ext == "java"
                || ext == "xml"
                || ext == "properties"
                || ext == "txt" && !name.endsWith(".results.txt")
            }

            for (file in extraFiles) {
                myFixture.configureByFile(rootPath + file.name)
            }
            myFixture.configureByFile(path)

            val caretElement = if (InTextDirectivesUtils.isDirectiveDefined(mainFileText, "// FIND_BY_REF"))
                TargetElementUtilBase.findTargetElement(myFixture.editor,
                                                        TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED or JavaTargetElementEvaluator.NEW_AS_CONSTRUCTOR)!!
            else
                myFixture.elementAtCaret
            UsefulTestCase.assertInstanceOf(caretElement, caretElementClass)

            val containingFile = caretElement.containingFile
            val isLibraryElement = containingFile != null && ProjectRootsUtil.isLibraryFile(project, containingFile.virtualFile)

            val options = parser?.parse(mainFileText, project)

            // Ensure that search by sources (if present) and decompiled declarations gives the same results
            if (isLibraryElement) {
                val originalElement = caretElement.originalElement
                findUsagesAndCheckResults(mainFileText, prefix, rootPath, originalElement, options)

                val navigationElement = caretElement.navigationElement
                if (navigationElement !== originalElement) {
                    findUsagesAndCheckResults(mainFileText, prefix, rootPath, navigationElement, options)
                }
            }
            else {
                findUsagesAndCheckResults<PsiElement>(mainFileText, prefix, rootPath, caretElement, options)
            }
        }
        finally {
            fixtureClasses.forEach { TestFixtureExtension.unloadFixture(it) }
        }
    }

    private fun <T : PsiElement> findUsagesAndCheckResults(
            mainFileText: String,
            prefix: String,
            rootPath: String,
            caretElement: T,
            options: FindUsagesOptions?
    ) {
        val highlightingMode = InTextDirectivesUtils.isDirectiveDefined(mainFileText, "// HIGHLIGHTING")

        var log: String? = null
        val logList = ArrayList<String>()
        val usageInfos = try {
            if (ExpressionsOfTypeProcessor.mode !== ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN) {
                ExpressionsOfTypeProcessor.testLog = logList
            }

            findUsages(caretElement, options, highlightingMode)
        }
        finally {
            ExpressionsOfTypeProcessor.testLog = null
            if (logList.size > 0) {
                log = logList.sorted().joinToString("\n")
            }
        }

        val filteringRules = instantiateClasses<UsageFilteringRule>(mainFileText, "// FILTERING_RULES: ")
        val groupingRules = instantiateClasses<UsageGroupingRule>(mainFileText, "// GROUPING_RULES: ")

        val filteredUsages = getUsageAdapters(filteringRules, usageInfos)

        val usageFiles = filteredUsages.map { it.file.name }.distinct()
        val appendFileName = usageFiles.size > 1

        val convertToString: (UsageInfo2UsageAdapter) -> String = { usageAdapter ->
            var groupAsString = groupingRules
                    .map { it.groupUsage(usageAdapter)?.getText(null) ?: "" }
                    .joinToString(", ")
            if (!groupAsString.isEmpty()) {
                groupAsString = "($groupAsString) "
            }

            val usageType = getUsageType(usageAdapter.element)
            val usageTypeAsString = usageType?.toString(USAGE_VIEW_PRESENTATION) ?: "null"

            val usageChunks = ArrayList<TextChunk>()
            usageChunks.addAll(usageAdapter.presentation.text.asList())
            usageChunks.add(1, TextChunk(TextAttributes(), " ")) // add space after line number

            buildString {
                if (appendFileName) {
                    append("[").append(usageAdapter.file.name).append("] ")
                }
                append(usageTypeAsString)
                append(" ")
                append(groupAsString)
                append(usageChunks.joinToString(""))
            }
        }

        val finalUsages = filteredUsages.map(convertToString).sorted()
        KotlinTestUtils.assertEqualsToFile(File(rootPath, prefix + "results.txt"), finalUsages.joinToString("\n"))

        if (log != null) {
            KotlinTestUtils.assertEqualsToFile(File(rootPath, prefix + "log"), log)

            // if log is empty then compare results with plain search
            try {
                ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN

                findUsagesAndCheckResults(mainFileText, prefix, rootPath, caretElement, options)
            }
            finally {
                ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART
            }
        }
    }

    protected fun findUsages(
            targetElement: PsiElement,
            options: FindUsagesOptions?,
            highlightingMode: Boolean
    ): Collection<UsageInfo> {
        val project = project

        val handler: FindUsagesHandler = (if (targetElement is PsiMember) {
            JavaFindUsagesHandler(targetElement, JavaFindUsagesHandlerFactory(project))
        }
        else if (targetElement is KtDeclaration && targetElement !is KtTypeAlias) {
            KotlinFindUsagesHandlerFactory(project).createFindUsagesHandlerNoQuestions(targetElement)
        }
        else {
            (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(targetElement, false)
        }) ?: error("Cannot find handler for: $targetElement")

        @Suppress("NAME_SHADOWING")
        val options = options ?: handler.getFindUsagesOptions(null)

        options.searchScope = GlobalSearchScope.allScope(project)

        val processor = CommonProcessors.CollectProcessor<UsageInfo>()
        for (psiElement in handler.primaryElements + handler.secondaryElements) {
            if (highlightingMode) {
                //TODO: should findReferencesToHighlight work outside read-action or it makes no sense?
                for (reference in handler.findReferencesToHighlight(psiElement, options.searchScope)) {
                    processor.process(UsageInfo(reference))
                }
            }
            else {
                // run in another thread to test read-action assertions
                val thread = Thread {
                    handler.processElementUsages(psiElement, processor, options)
                }
                thread.start()
                thread.join()
            }
        }

        return processor.results
    }

    companion object {

        val USAGE_VIEW_PRESENTATION = UsageViewPresentation()

        private fun getUsageAdapters(
                filters: Collection<UsageFilteringRule>,
                usageInfos: Collection<UsageInfo>
        ): Collection<UsageInfo2UsageAdapter> {
            return usageInfos
                    .map(::UsageInfo2UsageAdapter)
                    .filter { usageAdapter -> filters.all { it.isVisible(usageAdapter) } }
        }

        private fun getUsageType(element: PsiElement?): UsageType? {
            if (element == null) return null

            if (element.getNonStrictParentOfType<PsiComment>() != null) {
                return UsageType.COMMENT_USAGE
            }

            val providers = Extensions.getExtensions(UsageTypeProvider.EP_NAME)
            return providers
                           .mapNotNull { it.getUsageType(element) }
                           .firstOrNull()
                   ?: UsageType.UNCLASSIFIED
        }

        private fun <T> instantiateClasses(mainFileText: String, directive: String): Collection<T> {
            val filteringRuleClassNames = InTextDirectivesUtils.findLinesWithPrefixesRemoved(mainFileText, directive)
            return filteringRuleClassNames
                    .map {
                        @Suppress("UNCHECKED_CAST")
                        (Class.forName(it).newInstance() as T)
                    }
        }
    }
}
