/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPassFactory
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.*
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ArrayUtilRt
import com.intellij.util.ThrowableRunnable
import com.intellij.util.indexing.UnindexedFilesUpdater
import com.intellij.util.io.exists
import org.jetbrains.kotlin.idea.configuration.getModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.perf.Stats.Companion.WARM_UP
import org.jetbrains.kotlin.idea.perf.Stats.Companion.runAndMeasure
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.idea.testFramework.*
import org.jetbrains.kotlin.idea.testFramework.Fixture.Companion.cleanupCaches
import org.jetbrains.kotlin.idea.testFramework.Fixture.Companion.isAKotlinScriptFile
import org.jetbrains.kotlin.idea.testFramework.Fixture.Companion.openFileInEditor
import org.jetbrains.kotlin.idea.testFramework.Fixture.Companion.openFixture
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import java.io.File
import java.nio.file.Paths

abstract class AbstractPerformanceProjectsTest : UsefulTestCase() {

    // myProject is not required for all potential perf test cases
    protected var myProject: Project? = null
    private lateinit var jdk18: Sdk
    private lateinit var myApplication: TestApplicationManager

    override fun isStressTest(): Boolean = true

    override fun isPerformanceTest(): Boolean = false

    override fun setUp() {
        super.setUp()

        myApplication = TestApplicationManager.getInstance()
        runWriteAction {
            val jdkTableImpl = JavaAwareProjectJdkTableImpl.getInstanceEx()
            val homePath = if (jdkTableImpl.internalJdk.homeDirectory!!.name == "jre") {
                jdkTableImpl.internalJdk.homeDirectory!!.parent.path
            } else {
                jdkTableImpl.internalJdk.homePath!!
            }

            val javaSdk = JavaSdk.getInstance()
            jdk18 = javaSdk.createJdk("1.8", homePath)
            val internal = javaSdk.createJdk("IDEA jdk", homePath)

            val jdkTable = getProjectJdkTableSafe()
            jdkTable.addJdk(jdk18, testRootDisposable)
            jdkTable.addJdk(internal, testRootDisposable)
            KotlinSdkType.setUpIfNeeded()
        }
    }

    protected fun warmUpProject(stats: Stats) {
        val project = perfOpenHelloWorld(stats, WARM_UP)
        try {
            val perfHighlightFile = perfHighlightFile(project, "src/HelloMain.kt", stats, WARM_UP)
            assertTrue("kotlin project has been not imported properly", perfHighlightFile.isNotEmpty())
        } finally {
            closeProject(project)
            myApplication.setDataProvider(null)
        }
    }

    override fun tearDown() {
        commitAllDocuments()
        RunAll(
            ThrowableRunnable { super.tearDown() },
            ThrowableRunnable {
                if (myProject != null) {
                    DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled = true // return default value to avoid unnecessary save
                    (StartupManager.getInstance(project()) as StartupManagerImpl).checkCleared()
                    (DaemonCodeAnalyzer.getInstance(project()) as DaemonCodeAnalyzerImpl).cleanupAfterTest()
                    closeProject(project())
                    myApplication.setDataProvider(null)
                    myProject = null
                }
            }).run()
    }

    fun simpleFilename(fileName: String): String {
        val lastIndexOf = fileName.lastIndexOf('/')
        return if (lastIndexOf >= 0) fileName.substring(lastIndexOf + 1) else fileName
    }

    protected fun perfOpenKotlinProjectFast(stats: Stats) =
        perfOpenKotlinProject(stats, fast = true)

    protected fun perfOpenKotlinProject(stats: Stats, fast: Boolean = false) {
        myProject = innerPerfOpenProject("kotlin", stats = stats, path = "../perfTestProject", note = "", fast = fast)
    }

    protected fun perfOpenHelloWorld(stats: Stats, note: String = ""): Project =
        innerPerfOpenProject("helloKotlin", stats, note, path = "idea/testData/perfTest/helloKotlin", simpleModule = true)

    protected fun innerPerfOpenProject(
        name: String,
        stats: Stats,
        note: String,
        path: String,
        simpleModule: Boolean = false,
        fast: Boolean = false
    ): Project {
        val projectPath = File(path).canonicalPath

        assertTrue("path $path does not exist, check README.md", File(projectPath).exists())

        val warmUpIterations = if (fast) 0 else 1
        val iterations = if (fast) 1 else 3
        val projectManagerEx = ProjectManagerEx.getInstanceEx()

        var lastProject: Project? = null
        var counter = 0

        performanceTest<Unit, Project> {
            name("open project${if (note.isNotEmpty()) " $note" else ""}")
            stats(stats)
            warmUpIterations(warmUpIterations)
            iterations(iterations)
            test {
                val project = if (!simpleModule) {
                    val project = loadProjectWithName(name = name, path = path)
                    assertNotNull("project $name at $path is not loaded", project)
                    val projectRootManager = ProjectRootManager.getInstance(project!!)

                    runWriteAction {
                        with(projectRootManager) {
                            projectSdk = jdk18
                        }
                    }
                    assertTrue("project $name at $path is not opened", projectManagerEx.openProject(project))
                    project
                } else {
                    val project = projectManagerEx.loadAndOpenProject(projectPath)!!
                    initKotlinProject(project, projectPath, name)
                    project
                }

                (project as ProjectImpl).registerComponentImplementation(
                    FileEditorManager::class.java,
                    FileEditorManagerImpl::class.java
                )

                dispatchAllInvocationEvents()

                runStartupActivities(project)

                logMessage { "project $name is ${if (project.isInitialized) "initialized" else "not initialized"}" }

                with(ChangeListManager.getInstance(project) as ChangeListManagerImpl) {
                    waitUntilRefreshed()
                }

                it.value = project
            }
            tearDown {
                it.value?.let { project ->

                    runAndMeasure("refresh gradle project $name") {
                        refreshGradleProjectIfNeeded(projectPath, project)
                    }

                    ApplicationManager.getApplication().executeOnPooledThread {
                        DumbService.getInstance(project).waitForSmartMode()

                        for (module in getModulesWithKotlinFiles(project)) {
                            module.getAndCacheLanguageLevelByDependencies()
                        }
                    }.get()

                    val modules = ModuleManager.getInstance(project).modules
                    assertTrue("project $name has to have at least one module", modules.isNotEmpty())

                    logMessage { "modules of $name: ${modules.map { m -> m.name }}" }

                    lastProject = project
                    VirtualFileManager.getInstance().syncRefresh()

                    runWriteAction {
                        project.save()
                    }

                    logMessage { "project '$name' successfully opened" }

                    // close all project but last - we're going to return and use it further
                    if (counter < warmUpIterations + iterations - 1) {
                        closeProject(project)
                        myApplication.setDataProvider(null)
                    }
                    counter++
                }
            }
        }

        // indexing
        lastProject?.let { project ->
            invalidateLibraryCache(project)

            CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

            dispatchAllInvocationEvents()

            logMessage { "project $name is ${if (project.isInitialized) "initialized" else "not initialized"}" }

            with(DumbService.getInstance(project)) {
                queueTask(UnindexedFilesUpdater(project))
                completeJustSubmittedTasks()
            }
            dispatchAllInvocationEvents()

            Fixture.enableAnnotatorsAndLoadDefinitions(project)

            myApplication.setDataProvider(TestDataProvider(project))
        }

        return lastProject ?: error("unable to open project $name at $path")
    }

    private fun refreshGradleProjectIfNeeded(projectPath: String, project: Project) {
        if (listOf("build.gradle.kts", "build.gradle").map { name -> Paths.get(projectPath, name).exists() }.find { e -> e } != true) return

        ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(true)

        refreshGradleProject(projectPath, project)

        dispatchAllInvocationEvents()

        // WARNING: [VD] DO NOT SAVE PROJECT AS IT COULD PERSIST WRONG MODULES INFO

//        runInEdtAndWait {
//            PlatformTestUtil.saveProject(project)
//        }
    }

    fun perfTypeAndAutocomplete(
        stats: Stats,
        fileName: String,
        marker: String,
        insertString: String,
        surroundItems: String = "\n",
        lookupElements: List<String>,
        typeAfterMarker: Boolean = true,
        revertChangesAtTheEnd: Boolean = true,
        note: String = ""
    ) = perfTypeAndAutocomplete(
        project(), stats, fileName, marker, insertString, surroundItems,
        lookupElements = lookupElements, typeAfterMarker = typeAfterMarker,
        revertChangesAtTheEnd = revertChangesAtTheEnd, note = note
    )

    fun perfTypeAndAutocomplete(
        project: Project,
        stats: Stats,
        fileName: String,
        marker: String,
        insertString: String,
        surroundItems: String = "\n",
        lookupElements: List<String>,
        typeAfterMarker: Boolean = true,
        revertChangesAtTheEnd: Boolean = true,
        note: String = ""
    ) {
        assertTrue("lookupElements has to be not empty", lookupElements.isNotEmpty())
        performanceTest<Pair<String, Fixture>, Array<LookupElement>> {
            name("typeAndAutocomplete ${notePrefix(note)}$fileName")
            stats(stats)
            warmUpIterations(8)
            iterations(15)
            setUp {
                val fixture = openFixture(project, fileName)
                val editor = fixture.editor

                val initialText = editor.document.text
                updateScriptDependenciesIfNeeded(fileName, fixture, project)

                val tasksIdx = editor.document.text.indexOf(marker)
                assertTrue("marker '$marker' not found in $fileName", tasksIdx > 0)
                if (typeAfterMarker) {
                    editor.caretModel.moveToOffset(tasksIdx + marker.length + 1)
                } else {
                    editor.caretModel.moveToOffset(tasksIdx - 1)
                }

                for (surroundItem in surroundItems) {
                    EditorTestUtil.performTypingAction(editor, surroundItem)
                }

                editor.caretModel.moveToOffset(editor.caretModel.offset - if (typeAfterMarker) 1 else 2)

                if (!typeAfterMarker) {
                    for (surroundItem in surroundItems) {
                        EditorTestUtil.performTypingAction(editor, surroundItem)
                    }
                    editor.caretModel.moveToOffset(editor.caretModel.offset - 2)
                }

                fixture.type(insertString)

                it.setUpValue = Pair(initialText, fixture)
            }
            test {
                val fixture = it.setUpValue!!.second
                it.value = fixture.complete()
            }
            tearDown {
                val items = it.value?.map { e -> e.lookupString }?.toList() ?: emptyList()
                try {
                    for (lookupElement in lookupElements) {
                        assertTrue("'$lookupElement' has to be present in items", items.contains(lookupElement))
                    }
                } finally {
                    it.setUpValue?.let { pair ->
                        pair.second.revertChanges(revertChangesAtTheEnd, pair.first)
                    }
                    commitAllDocuments()
                }
            }
            profileEnabled(true)
        }
    }

    fun perfTypeAndHighlight(
        stats: Stats,
        fileName: String,
        marker: String,
        insertString: String,
        surroundItems: String = "\n",
        typeAfterMarker: Boolean = true,
        revertChangesAtTheEnd: Boolean = true,
        note: String = ""
    ) = perfTypeAndHighlight(
        project(), stats, fileName, marker, insertString, surroundItems,
        typeAfterMarker = typeAfterMarker,
        revertChangesAtTheEnd = revertChangesAtTheEnd, note = note
    )

    fun perfTypeAndHighlight(
        project: Project,
        stats: Stats,
        fileName: String,
        marker: String,
        insertString: String,
        surroundItems: String = "\n",
        typeAfterMarker: Boolean = true,
        revertChangesAtTheEnd: Boolean = true,
        note: String = ""
    ) {
        performanceTest<Pair<String, Fixture>, List<HighlightInfo>> {
            name("typeAndHighlight ${notePrefix(note)}$fileName")
            stats(stats)
            warmUpIterations(8)
            iterations(15)
            setUp {
                val fixture = openFixture(project, fileName)
                val editor = fixture.editor

                val initialText = editor.document.text
                updateScriptDependenciesIfNeeded(fileName, fixture, project)

                val tasksIdx = editor.document.text.indexOf(marker)
                assertTrue("marker '$marker' not found in $fileName", tasksIdx > 0)
                if (typeAfterMarker) {
                    editor.caretModel.moveToOffset(tasksIdx + marker.length + 1)
                } else {
                    editor.caretModel.moveToOffset(tasksIdx - 1)
                }

                for (surroundItem in surroundItems) {
                    EditorTestUtil.performTypingAction(editor, surroundItem)
                }

                editor.caretModel.moveToOffset(editor.caretModel.offset - if (typeAfterMarker) 1 else 2)

                if (!typeAfterMarker) {
                    for (surroundItem in surroundItems) {
                        EditorTestUtil.performTypingAction(editor, surroundItem)
                    }
                    editor.caretModel.moveToOffset(editor.caretModel.offset - 2)
                }

                fixture.type(insertString)

                it.setUpValue = Pair(initialText, fixture)
            }
            test {
                val fixture = it.setUpValue!!.second
                it.value = fixture.doHighlighting()
            }
            tearDown {
                it.value?.let { list ->
                    assertNotEmpty(list)
                }
                it.setUpValue?.let { pair ->
                    pair.second.revertChanges(revertChangesAtTheEnd, pair.first)
                }
                commitAllDocuments()
            }
            profileEnabled(true)
        }
    }

    fun perfCopyAndPaste(
        stats: Stats,
        sourceFileName: String,
        sourceInitialMarker: String? = null,
        sourceFinalMarker: String? = null,
        targetFileName: String,
        targetInitialMarker: String? = null,
        targetFinalMarker: String? = null,
        note: String = ""
    ) = perfCopyAndPaste(
        project(), stats,
        sourceFileName, sourceInitialMarker, sourceFinalMarker,
        targetFileName, targetInitialMarker, targetFinalMarker,
        note
    )

    fun perfCopyAndPaste(
        project: Project,
        stats: Stats,
        sourceFileName: String,
        sourceInitialMarker: String? = null,
        sourceFinalMarker: String? = null,
        targetFileName: String,
        targetInitialMarker: String? = null,
        targetFinalMarker: String? = null,
        note: String = ""
    ) {
        performanceTest<Pair<Array<Fixture>, String>, Boolean> {
            name("${notePrefix(note)}$sourceFileName")
            stats(stats)
            warmUpIterations(8)
            iterations(15)
            setUp {
                val fixture1 = openFixture(project, sourceFileName)
                val fixture2 = openFixture(project, targetFileName)

                val initialText2 = fixture2.document.text

                updateScriptDependenciesIfNeeded(sourceFileName, fixture1, project)
                updateScriptDependenciesIfNeeded(sourceFileName, fixture2, project)

                fixture1.selectMarkers(sourceInitialMarker, sourceFinalMarker)
                fixture2.selectMarkers(targetInitialMarker, targetFinalMarker)

                it.setUpValue = Pair(arrayOf(fixture1, fixture2), initialText2)
            }
            test {
                it.setUpValue?.let { setUpValue ->
                    val fixture1 = setUpValue.first[0]
                    val fixture2 = setUpValue.first[1]
                    it.value = fixture1.performEditorAction(IdeActions.ACTION_COPY) &&
                            fixture2.performEditorAction(IdeActions.ACTION_PASTE)
                }
            }
            tearDown {
                try {
                    commitAllDocuments()
                    it.value?.let { performed ->
                        assertTrue("copy-n-paste has not performed well", performed)
                        // files could be different due to spaces
                        //assertEquals(it.setUpValue!!.first.document.text, it.setUpValue!!.second.document.text)
                    }
                } finally {
                    it.setUpValue?.let { setUpValue ->
                        // pair.second.performEditorAction(IdeActions.ACTION_UNDO)
                        val fixture2 = setUpValue.first[1]
                        fixture2.applyText(setUpValue.second)
                    }
                    commitAllDocuments()
                }
            }
            profileEnabled(true)
        }
    }

    private fun updateScriptDependenciesIfNeeded(
        fileName: String,
        fixture: Fixture,
        project: Project
    ) {
        if (isAKotlinScriptFile(fileName)) {
            runAndMeasure("update script dependencies for $fileName") {
                ScriptConfigurationManager.updateScriptDependenciesSynchronously(fixture.psiFile, project)
            }
        }
    }

    private fun initKotlinProject(
        project: Project,
        projectPath: String,
        name: String
    ) {
        val modulePath = "$projectPath/$name${ModuleFileType.DOT_DEFAULT_EXTENSION}"
        val projectFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectPath))!!
        val srcFile = projectFile.findChild("src")!!
        val module = runWriteAction {
            val projectRootManager = ProjectRootManager.getInstance(project)
            with(projectRootManager) {
                projectSdk = jdk18
            }
            val moduleManager = ModuleManager.getInstance(project)
            val module = moduleManager.newModule(modulePath, ModuleTypeId.JAVA_MODULE)
            PsiTestUtil.addSourceRoot(module, srcFile)
            module
        }
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, jdk18)
    }

    protected fun perfHighlightFile(name: String, stats: Stats): List<HighlightInfo> =
        perfHighlightFile(project(), name, stats)

    protected fun perfHighlightFile(
        project: Project,
        fileName: String,
        stats: Stats,
        note: String = ""
    ): List<HighlightInfo> {
        return highlightFile {
            val isWarmUp = note == WARM_UP
            var highlightInfos: List<HighlightInfo> = emptyList()
            performanceTest<EditorFile, List<HighlightInfo>> {
                name("highlighting ${notePrefix(note)}${simpleFilename(fileName)}")
                stats(stats)
                warmUpIterations(if (isWarmUp) 1 else 3)
                iterations(if (isWarmUp) 2 else 10)
                setUp {
                    it.setUpValue = openFileInEditor(project, fileName)
                }
                test {
                    val file = it.setUpValue
                    it.value = highlightFile(project, file!!.psiFile)
                }
                tearDown {
                    highlightInfos = it.value ?: emptyList()
                    commitAllDocuments()
                    FileEditorManager.getInstance(project).closeFile(it.setUpValue!!.psiFile.virtualFile)
                    PsiManager.getInstance(project).dropPsiCaches()
                }
                profileEnabled(!isWarmUp)
            }
            highlightInfos
        }
    }

    fun <T> highlightFile(block: () -> T): T {
        var value: T? = null
        IdentifierHighlighterPassFactory.doWithHighlightingEnabled {
            value = block()
        }
        return value!!
    }

    private fun highlightFile(project: Project, psiFile: PsiFile): List<HighlightInfo> {
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile)!!
        val editor = EditorFactory.getInstance().getEditors(document).first()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        return CodeInsightTestFixtureImpl.instantiateAndRun(psiFile, editor, ArrayUtilRt.EMPTY_INT_ARRAY, true)
    }

    protected fun perfScriptDependencies(name: String, stats: Stats, note: String = "") =
        perfScriptDependencies(project(), name, stats, note = note)

    private fun project() = myProject ?: error("project has not been initialized")

    private fun perfScriptDependencies(
        project: Project,
        fileName: String,
        stats: Stats,
        note: String = ""
    ) {
        if (!isAKotlinScriptFile(fileName)) return
        performanceTest<EditorFile, EditorFile> {
            name("updateScriptDependencies ${notePrefix(note)}${simpleFilename(fileName)}")
            stats(stats)
            setUp { it.setUpValue = openFileInEditor(project, fileName) }
            test {
                ScriptConfigurationManager.updateScriptDependenciesSynchronously(it.setUpValue!!.psiFile, project)
                it.value = it.setUpValue
            }
            tearDown {
                it.setUpValue?.let { ef -> cleanupCaches(project, ef.psiFile.virtualFile) }
                it.value?.let { v -> assertNotNull(v) }
            }
            profileEnabled(true)
        }
    }

    fun notePrefix(note: String) = if (note.isNotEmpty()) {
        if (note.endsWith("/")) note else "$note "
    } else ""


}