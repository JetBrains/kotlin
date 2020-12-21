/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.cache.CacheManager
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.io.exists
import gnu.trove.TIntArrayList
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import org.jetbrains.kotlin.checkers.diagnostics.DebugInfoDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.SyntaxErrorDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory0
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.checkers.utils.DiagnosticsRenderingConfiguration
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoRenderer
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.daemon.common.OSKind
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.AbstractDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeMetaInfo.models.*
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.HighlightingRenderConfiguration
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.LineMarkerRenderConfiguration
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromTextFile
import org.jetbrains.kotlin.idea.resolve.getDataFlowValueFactory
import org.jetbrains.kotlin.idea.resolve.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Ignore
import java.io.File
import java.nio.file.Paths

@Ignore
class CodeMetaInfoTestCase(
    val codeMetaInfoTypes: Collection<AbstractCodeMetaInfoRenderConfiguration>,
    val checkNoDiagnosticError: Boolean = false
) : DaemonAnalyzerTestCase() {

    fun getDiagnosticCodeMetaInfos(
        configuration: DiagnosticCodeMetaInfoRenderConfiguration = DiagnosticCodeMetaInfoRenderConfiguration(),
        parseDirective: Boolean = true
    ): List<CodeMetaInfo> {
        val tempSourceKtFile = PsiManager.getInstance(project).findFile(file.virtualFile) as KtFile
        val resolutionFacade = tempSourceKtFile.getResolutionFacade()
        val (bindingContext, moduleDescriptor, _) = resolutionFacade.analyzeWithAllCompilerChecks(listOf(tempSourceKtFile))
        val directives = KotlinTestUtils.parseDirectives(file.text)
        val diagnosticsFilter = BaseDiagnosticsTest.parseDiagnosticFilterDirective(directives, allowUnderscoreUsage = false)
        val diagnostics = CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(
            bindingContext,
            file,
            markDynamicCalls = false,
            dynamicCallDescriptors = mutableListOf(),
            configuration = DiagnosticsRenderingConfiguration(
                platform = null, // we don't need to attach platform-description string to diagnostic here
                withNewInference = false,
                languageVersionSettings = resolutionFacade.getLanguageVersionSettings(),
            ),
            dataFlowValueFactory = resolutionFacade.getDataFlowValueFactory(),
            moduleDescriptor = moduleDescriptor as ModuleDescriptorImpl
        ).map { it.diagnostic }.filter { !parseDirective || diagnosticsFilter.value(it) }
        configuration.renderParams = directives.contains(BaseDiagnosticsTest.RENDER_DIAGNOSTICS_MESSAGES)
        return getCodeMetaInfo(diagnostics, configuration)
    }

    fun getLineMarkerCodeMetaInfos(configuration: LineMarkerRenderConfiguration): Collection<CodeMetaInfo> {
        if ("!CHECK_HIGHLIGHTING" in file.text)
            return emptyList()

        CodeInsightTestFixtureImpl.instantiateAndRun(file, editor, TIntArrayList().toNativeArray(), false)
        val lineMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(getDocument(file), project)
        return getCodeMetaInfo(lineMarkers, configuration)
    }

    fun getHighlightingCodeMetaInfos(configuration: HighlightingRenderConfiguration): Collection<CodeMetaInfo> {
        val infos = CodeInsightTestFixtureImpl.instantiateAndRun(file, editor, TIntArrayList().toNativeArray(), false)

        return getCodeMetaInfo(infos, configuration)
    }

    fun checkFile(expectedFile: File, project: Project, editor: Editor) {
        myProject = project
        myPsiManager = PsiManager.getInstance(myProject) as PsiManagerImpl
        runInEdtAndWait {
            setActiveEditor(editor)
            check(expectedFile)
        }
    }

    fun checkFile(file: VirtualFile, expectedFile: File, project: Project) {
        myProject = project
        myPsiManager = PsiManager.getInstance(myProject) as PsiManagerImpl
        configureByExistingFile(file)
        check(expectedFile)
    }

    fun check(expectedFile: File) {
        val codeMetaInfoForCheck = mutableListOf<CodeMetaInfo>()
        PsiDocumentManager.getInstance(myProject).commitAllDocuments()

        //to load text
        ApplicationManager.getApplication().runWriteAction { TreeUtil.clearCaches(myFile.node as TreeElement) }

        //to initialize caches
        if (!DumbService.isDumb(myProject)) {
            CacheManager.SERVICE.getInstance(myProject)
                .getFilesWithWord("XXX", UsageSearchContext.IN_COMMENTS, GlobalSearchScope.allScope(myProject), true)
        }

        for (configuration in codeMetaInfoTypes) {
            when (configuration) {
                is DiagnosticCodeMetaInfoRenderConfiguration -> {
                    codeMetaInfoForCheck.addAll(getDiagnosticCodeMetaInfos(configuration))
                }
                is HighlightingRenderConfiguration -> {
                    codeMetaInfoForCheck.addAll(getHighlightingCodeMetaInfos(configuration))
                }
                is LineMarkerRenderConfiguration -> {
                    codeMetaInfoForCheck.addAll(getLineMarkerCodeMetaInfos(configuration))
                }
                else -> throw IllegalArgumentException("Unexpected code meta info configuration: $configuration")
            }
        }
        if (codeMetaInfoTypes.any { it is DiagnosticCodeMetaInfoRenderConfiguration } &&
            !codeMetaInfoTypes.any { it is HighlightingRenderConfiguration }
        ) {
            checkHighlightErrorItemsInDiagnostics(
                getDiagnosticCodeMetaInfos(DiagnosticCodeMetaInfoRenderConfiguration(), false).filterIsInstance<DiagnosticCodeMetaInfo>()
            )
        }
        val parsedMetaInfo = CodeMetaInfoParser.getCodeMetaInfoFromText(expectedFile.readText()).toMutableList()
        codeMetaInfoForCheck.forEach { codeMetaInfo ->
            val correspondingParsed = parsedMetaInfo.firstOrNull { it == codeMetaInfo }
            if (correspondingParsed != null) {
                parsedMetaInfo.remove(correspondingParsed)
                codeMetaInfo.attributes.addAll(correspondingParsed.attributes)
                if (correspondingParsed.attributes.isNotEmpty() && OSKind.current.toString() !in correspondingParsed.attributes)
                    codeMetaInfo.attributes.add(OSKind.current.toString())
            }
        }
        parsedMetaInfo.forEach {
            if (it.attributes.isNotEmpty() && OSKind.current.toString() !in it.attributes) codeMetaInfoForCheck.add(
                it
            )
        }
        val textWithCodeMetaInfo = CodeMetaInfoRenderer.renderTagsToText(codeMetaInfoForCheck, myEditor.document.text)
        KotlinTestUtils.assertEqualsToFile(
            expectedFile,
            textWithCodeMetaInfo.toString()
        )

        if (checkNoDiagnosticError) {
            val diagnosticsErrors =
                getDiagnosticCodeMetaInfos().filter { (it as DiagnosticCodeMetaInfo).diagnostic.severity == Severity.ERROR }
            assertTrue(
                "Diagnostics with severity ERROR were found: ${diagnosticsErrors.joinToString { it.asString() }}",
                diagnosticsErrors.isEmpty()
            )
        }
    }

    private fun checkHighlightErrorItemsInDiagnostics(
        diagnostics: Collection<DiagnosticCodeMetaInfo>
    ) {
        val highlightItems: List<CodeMetaInfo> =
            getHighlightingCodeMetaInfos(HighlightingRenderConfiguration()).filter { (it as HighlightingCodeMetaInfo).highlightingInfo.severity == HighlightSeverity.ERROR }

        highlightItems.forEach { highlightingCodeMetaInfo ->
            assert(
                diagnostics.any { diagnosticCodeMetaInfo ->
                    diagnosticCodeMetaInfo.start == highlightingCodeMetaInfo.start &&
                            when (val diagnostic = diagnosticCodeMetaInfo.diagnostic) {
                                is SyntaxErrorDiagnostic -> {
                                    (highlightingCodeMetaInfo as HighlightingCodeMetaInfo).highlightingInfo.description in (diagnostic.psiElement as PsiErrorElementImpl).errorDescription
                                }
                                is AbstractDiagnostic<*> -> {
                                    diagnostic.factory.toString() in (highlightingCodeMetaInfo as HighlightingCodeMetaInfo).highlightingInfo.description
                                }
                                is DebugInfoDiagnostic -> {
                                    diagnostic.factory == DebugInfoDiagnosticFactory0.MISSING_UNRESOLVED &&
                                            "[DEBUG] Reference is not resolved to anything, but is not marked unresolved" in (highlightingCodeMetaInfo as HighlightingCodeMetaInfo).highlightingInfo.description
                                }
                                else -> throw java.lang.IllegalArgumentException("Unknown diagnostic type: ${diagnosticCodeMetaInfo.diagnostic}")
                            }
                },
            ) { "Could not find DIAGNOSTIC for ${(highlightingCodeMetaInfo as HighlightingCodeMetaInfo).highlightingInfo}" }
        }
    }
}

abstract class AbstractDiagnosticCodeMetaInfoTest : AbstractCodeMetaInfoTest() {
    override fun getConfigurations() = listOf(
        DiagnosticCodeMetaInfoRenderConfiguration()
    )
}

abstract class AbstractLineMarkerCodeMetaInfoTest : AbstractCodeMetaInfoTest() {
    override fun getConfigurations() = listOf(
        LineMarkerRenderConfiguration()
    )
}

abstract class AbstractHighlightingCodeMetaInfoTest : AbstractCodeMetaInfoTest() {
    override fun getConfigurations() = listOf(
        HighlightingRenderConfiguration()
    )
}

abstract class AbstractCodeMetaInfoTest : AbstractMultiModuleTest() {
    open val checkNoDiagnosticError get() = false
    open fun getConfigurations() = listOf(
        DiagnosticCodeMetaInfoRenderConfiguration(),
        LineMarkerRenderConfiguration(),
        HighlightingRenderConfiguration()
    )

    protected open fun setupProject(testDataPath: String) {
        val dependenciesTxt = File(testDataPath, "dependencies.txt")
        require(dependenciesTxt.exists()) {
            "${dependenciesTxt.absolutePath} does not exist. dependencies.txt is required"
        }
        setupMppProjectFromTextFile(File(testDataPath))
    }

    fun doTest(testDataPath: String) {
        val testRoot = File(testDataPath)
        val checker = CodeMetaInfoTestCase(getConfigurations(), checkNoDiagnosticError)
        setupProject(testDataPath)

        for (module in ModuleManager.getInstance(project).modules) {
            for (sourceRoot in module.sourceRoots) {
                VfsUtilCore.processFilesRecursively(sourceRoot) { file ->
                    if (file.isDirectory) return@processFilesRecursively true

                    checker.checkFile(file, file.findCorrespondingFileInTestDir(sourceRoot, testRoot), project)
                    true
                }
            }
        }
    }

    private fun VirtualFile.findCorrespondingFileInTestDir(containingRoot: VirtualFile, testDir: File): File {
        val tempRootPath = Paths.get(containingRoot.path)
        val tempProjectDirPath = tempRootPath.parent
        val tempSourcePath = Paths.get(path)
        val relativeToProjectRootPath = tempProjectDirPath.relativize(tempSourcePath)
        val testSourcesProjectDirPath = testDir.toPath()
        val testSourcePath = testSourcesProjectDirPath.resolve(relativeToProjectRootPath)

        require(testSourcePath.exists()) {
            "Can't find file in testdata for copied file $this: checked at path ${testSourcePath.toAbsolutePath()}"
        }
        return testSourcePath.toFile()
    }
}
