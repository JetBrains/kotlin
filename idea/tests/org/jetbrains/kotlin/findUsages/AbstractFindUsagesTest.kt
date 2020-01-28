/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.find.FindManager
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.findUsages.JavaFindUsagesHandler
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import com.intellij.find.impl.FindManagerImpl
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
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
import org.jetbrains.kotlin.idea.core.util.clearDialogsResults
import org.jetbrains.kotlin.idea.core.util.setDialogsResult
import org.jetbrains.kotlin.idea.findUsages.handlers.KotlinFindMemberUsagesHandler
import org.jetbrains.kotlin.idea.refactoring.CHECK_SUPER_METHODS_YES_NO_DIALOG
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.runReadActionInSmartMode
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

abstract class AbstractFindUsagesWithDisableComponentSearchTest : AbstractFindUsagesTest() {

    override fun <T : PsiElement> doTest(path: String) {
        val oldValue = KotlinFindMemberUsagesHandler.forceDisableComponentAndDestructionSearch
        try {
            KotlinFindMemberUsagesHandler.forceDisableComponentAndDestructionSearch = true
            super.doTest<T>(path)
        } finally {
            KotlinFindMemberUsagesHandler.forceDisableComponentAndDestructionSearch = oldValue
        }
    }

    override val prefixForResults = "DisabledComponents."
}

abstract class AbstractFindUsagesTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    // used in Spring tests (outside main project!)
    protected open fun extraConfig(path: String) {
    }

    protected open val prefixForResults = ""

    protected open fun <T : PsiElement> doTest(path: String) {
        val mainFile = File(path)
        val mainFileName = mainFile.name
        val mainFileText = FileUtil.loadFile(mainFile, true)
        val prefix = mainFileName.substringBefore(".") + "."

        val isPropertiesFile = FileUtilRt.getExtension(path) == "properties"

        val isFindFileUsages = InTextDirectivesUtils.isDirectiveDefined(mainFileText, "## FIND_FILE_USAGES")

        @Suppress("UNCHECKED_CAST")
        val caretElementClass = (if (!isPropertiesFile) {
            val caretElementClassNames = InTextDirectivesUtils.findLinesWithPrefixesRemoved(mainFileText, "// PSI_ELEMENT: ")
            Class.forName(caretElementClassNames.single())
        } else if (isFindFileUsages) {
            PropertiesFile::class.java
        } else {
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
            val extraFiles = rootDir.listFiles { _, name ->
                if (!name.startsWith(prefix) || name == mainFileName) return@listFiles false

                val ext = FileUtilRt.getExtension(name)
                ext in SUPPORTED_EXTENSIONS && !name.endsWith(".results.txt")
            }.orEmpty()

            for (file in extraFiles) {
                myFixture.configureByFile(file.name)
            }
            myFixture.configureByFile(mainFileName)

            val caretElement = when {
                InTextDirectivesUtils.isDirectiveDefined(mainFileText, "// FIND_BY_REF") -> TargetElementUtil.findTargetElement(
                    myFixture.editor,
                    TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtil.getInstance().referenceSearchFlags
                )!!

                isFindFileUsages -> myFixture.file

                else -> myFixture.elementAtCaret
            }
            UsefulTestCase.assertInstanceOf(caretElement, caretElementClass)

            val containingFile = caretElement.containingFile
            val isLibraryElement = containingFile != null && ProjectRootsUtil.isLibraryFile(project, containingFile.virtualFile)

            val options = parser?.parse(mainFileText, project)

            // Ensure that search by sources (if present) and decompiled declarations gives the same results
            val prefixForCheck = prefix + prefixForResults
            if (isLibraryElement) {
                val originalElement = caretElement.originalElement
                findUsagesAndCheckResults(mainFileText, prefixForCheck, rootPath, originalElement, options, project)

                val navigationElement = caretElement.navigationElement
                if (navigationElement !== originalElement) {
                    findUsagesAndCheckResults(mainFileText, prefixForCheck, rootPath, navigationElement, options, project)
                }
            } else {
                findUsagesAndCheckResults(mainFileText, prefixForCheck, rootPath, caretElement, options, project)
            }
        } finally {
            fixtureClasses.forEach { TestFixtureExtension.unloadFixture(it) }
        }
    }


    companion object {
        val SUPPORTED_EXTENSIONS = setOf("kt", "kts", "java", "xml", "properties", "txt", "groovy")

        val USAGE_VIEW_PRESENTATION = UsageViewPresentation()

        internal fun getUsageAdapters(
            filters: Collection<UsageFilteringRule>,
            usageInfos: Collection<UsageInfo>
        ): Collection<UsageInfo2UsageAdapter> {
            return usageInfos
                .map(::UsageInfo2UsageAdapter)
                .filter { usageAdapter -> filters.all { it.isVisible(usageAdapter) } }
        }

        internal fun getUsageType(element: PsiElement?): UsageType? {
            if (element == null) return null

            if (element.getNonStrictParentOfType<PsiComment>() != null) {
                return UsageType.COMMENT_USAGE
            }

            @Suppress("DEPRECATION")
            val providers = Extensions.getExtensions(UsageTypeProvider.EP_NAME)

            return providers
                .mapNotNull {
                    it.getUsageType(element)
                }
                .firstOrNull()
                ?: UsageType.UNCLASSIFIED
        }

        internal fun <T> instantiateClasses(mainFileText: String, directive: String): Collection<T> {
            val filteringRuleClassNames = InTextDirectivesUtils.findLinesWithPrefixesRemoved(mainFileText, directive)
            return filteringRuleClassNames
                .map {
                    @Suppress("UNCHECKED_CAST")
                    (Class.forName(it).newInstance() as T)
                }
        }
    }
}

internal fun <T : PsiElement> findUsagesAndCheckResults(
    mainFileText: String,
    prefix: String,
    rootPath: String,
    caretElement: T,
    options: FindUsagesOptions?,
    project: Project,
    alwaysAppendFileName: Boolean = false
) {
    val highlightingMode = InTextDirectivesUtils.isDirectiveDefined(mainFileText, "// HIGHLIGHTING")

    var log: String? = null
    val logList = ArrayList<String>()
    val usageInfos = try {
        if (ExpressionsOfTypeProcessor.mode !== ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN) {
            ExpressionsOfTypeProcessor.testLog = logList
        }

        if (InTextDirectivesUtils.isDirectiveDefined(mainFileText, "// PLAIN_WHEN_NEEDED")) {
            ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.PLAIN_WHEN_NEEDED
        }

        val searchSuperDeclaration =
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(mainFileText, "$CHECK_SUPER_METHODS_YES_NO_DIALOG:").firstOrNull() != "no"

        findUsages(caretElement, options, highlightingMode, project, searchSuperDeclaration)
    } finally {
        ExpressionsOfTypeProcessor.testLog = null
        if (logList.size > 0) {
            log = logList.sorted().joinToString("\n")
        }

        ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART
    }

    val filteringRules = AbstractFindUsagesTest.instantiateClasses<UsageFilteringRule>(mainFileText, "// FILTERING_RULES: ")
    val groupingRules = AbstractFindUsagesTest.instantiateClasses<UsageGroupingRule>(mainFileText, "// GROUPING_RULES: ")

    val filteredUsages = AbstractFindUsagesTest.getUsageAdapters(filteringRules, usageInfos)

    val usageFiles = filteredUsages.map { it.file.name }.distinct()
    val appendFileName = alwaysAppendFileName || usageFiles.size > 1

    val convertToString: (UsageInfo2UsageAdapter) -> String = { usageAdapter ->
        var groupAsString = groupingRules.joinToString(", ") { it.groupUsage(usageAdapter)?.getText(null) ?: "" }
        if (groupAsString.isNotEmpty()) {
            groupAsString = "($groupAsString) "
        }

        val usageType = AbstractFindUsagesTest.getUsageType(usageAdapter.element)
        val usageTypeAsString = usageType?.toString(AbstractFindUsagesTest.USAGE_VIEW_PRESENTATION) ?: "null"

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

            findUsagesAndCheckResults(mainFileText, prefix, rootPath, caretElement, options, project)
        } finally {
            ExpressionsOfTypeProcessor.mode = ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART
        }
    }
}

internal fun findUsages(
    targetElement: PsiElement,
    options: FindUsagesOptions?,
    highlightingMode: Boolean,
    project: Project,
    searchSuperDeclaration: Boolean = true
): Collection<UsageInfo> {
    try {
        val handler: FindUsagesHandler = if (targetElement is PsiMember)
            JavaFindUsagesHandler(targetElement, JavaFindUsagesHandlerFactory(project))
        else {
            if (!searchSuperDeclaration) {
                setDialogsResult(CHECK_SUPER_METHODS_YES_NO_DIALOG, Messages.NO)
            }

            val findManagerImpl = FindManager.getInstance(project) as FindManagerImpl
            findManagerImpl.findUsagesManager.getFindUsagesHandler(targetElement, false)
                ?: error("Cannot find handler for: $targetElement")
        }

        @Suppress("NAME_SHADOWING")
        val options = options ?: handler.getFindUsagesOptions(null)

        options.searchScope = GlobalSearchScope.allScope(project)

        val processor = CommonProcessors.CollectProcessor<UsageInfo>()
        for (psiElement in handler.primaryElements + handler.secondaryElements) {
            if (highlightingMode) {
                project.runReadActionInSmartMode {
                    for (reference in handler.findReferencesToHighlight(psiElement, options.searchScope)) {
                        processor.process(UsageInfo(reference))
                    }
                }
            } else {
                ProgressManager.getInstance().run(
                    object : Task.Modal(project, "", false) {
                        override fun run(indicator: ProgressIndicator) {
                            project.runReadActionInSmartMode {
                                handler.processElementUsages(psiElement, processor, options)
                            }
                        }
                    },
                )
            }
        }

        return processor.results
    } finally {
        clearDialogsResults()
    }
}

