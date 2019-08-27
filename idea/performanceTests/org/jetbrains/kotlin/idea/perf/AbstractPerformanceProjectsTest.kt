/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.*
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPassFactory
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.idea.IdeaTestApplication
import com.intellij.lang.ExternalAnnotatorsFilter
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.StdLanguages
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFileNSInfoProvider
import com.intellij.testFramework.*
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ArrayUtilRt
import com.intellij.util.ThrowableRunnable
import com.intellij.util.indexing.UnindexedFilesUpdater
import com.intellij.util.io.exists
import com.intellij.xml.XmlSchemaProvider
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.configuration.getModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.idea.testFramework.*
import org.jetbrains.plugins.gradle.service.project.GradleProjectOpenProcessor
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths

abstract class AbstractPerformanceProjectsTest : UsefulTestCase() {

    // myProject is not required for all potential perf test cases
    protected var myProject: Project? = null
    private lateinit var jdk18: Sdk
    private lateinit var myApplication: IdeaTestApplication

    override fun isStressTest(): Boolean = true

    override fun isPerformanceTest(): Boolean = false

    override fun setUp() {
        super.setUp()

        myApplication = IdeaTestApplication.getInstance()
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

            val jdkTable = ProjectJdkTable.getInstance()
            jdkTable.addJdk(jdk18, testRootDisposable)
            jdkTable.addJdk(internal, testRootDisposable)
            KotlinSdkType.setUpIfNeeded()
        }
    }

    protected fun warmUpProject(stats: Stats) {
        val project = perfOpenHelloWorld(stats, "warm-up")
        try {
            val perfHighlightFile = perfHighlightFile(project, "src/HelloMain.kt", stats, "warm-up")
            assertTrue("kotlin project has been not imported properly", perfHighlightFile.isNotEmpty())
        } finally {
            closeProject(project)
        }
    }

    override fun tearDown() {
        commitAllDocuments()
        RunAll(
            ThrowableRunnable { super.tearDown() },
            ThrowableRunnable {
                if (myProject != null) {
                    DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled = true // return default value to avoid unnecessary save
                    (StartupManager.getInstance(myProject!!) as StartupManagerImpl).checkCleared()
                    (DaemonCodeAnalyzer.getInstance(myProject!!) as DaemonCodeAnalyzerImpl).cleanupAfterTest()
                    closeProject(myProject!!)
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
        val projectPath = File("$path").canonicalPath

        val warmUpIterations = if (fast) 0 else 1
        val iterations = if (fast) 1 else 3
        val projectManagerEx = ProjectManagerEx.getInstanceEx()

        var lastProject: Project? = null
        var counter = 0

        stats.perfTest<Unit, Project>(
            warmUpIterations = warmUpIterations,
            iterations = iterations,
            testName = "open project${if (note.isNotEmpty()) " $note" else ""}",
            test = {
                val project = if (!simpleModule) {
                    val project = projectManagerEx.loadProject(name, path)
                    assertNotNull(project)
                    //projectManagerEx.openTestProject(project!!)
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

                with(StartupManager.getInstance(project) as StartupManagerImpl) {
                    scheduleInitialVfsRefresh()
                    runPostStartupActivities()
                }

                with(ChangeListManager.getInstance(project) as ChangeListManagerImpl) {
                    waitUntilRefreshed()
                }

                it.value = project
            },
            tearDown = {
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

                    assertTrue(
                        "project has to have at least one module",
                        ModuleManager.getInstance(project).modules.isNotEmpty()
                    )

                    lastProject = project
                    VirtualFileManager.getInstance().syncRefresh()

                    runWriteAction {
                        project.save()
                    }

                    println("# project '$name' successfully opened")

                    // close all project but last - we're going to return and use it further
                    if (counter < warmUpIterations + iterations - 1) {
                        closeProject(project)
                    }
                    counter++
                }
            }
        )

        // indexing
        lastProject?.let { project ->
            invalidateLibraryCache(project)

            CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

            dispatchAllInvocationEvents()

            with(DumbService.getInstance(project)) {
                queueTask(UnindexedFilesUpdater(project))
                completeJustSubmittedTasks()
            }
            dispatchAllInvocationEvents()

            enableAnnotatorsAndLoadDefinitions(project)
        }

        return lastProject!!
    }

    fun openGradleProject(projectPath: String, project: Project) {
        dispatchAllInvocationEvents()

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(projectPath)!!

        FileDocumentManager.getInstance().saveAllDocuments()

        val path = Paths.get(virtualFile.path)
        GradleProjectOpenProcessor.openGradleProject(project, null, path)

        dispatchAllInvocationEvents()
        runInEdtAndWait {
            PlatformTestUtil.saveProject(project)
        }
    }

    private fun refreshGradleProjectIfNeeded(projectPath: String, project: Project) {
        if (listOf("build.gradle.kts", "build.gradle").map { name -> Paths.get(projectPath, name).exists() }.find { e -> e } != true) return

        ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(false)

        GradleProjectOpenProcessor.openGradleProject(project, null, Paths.get(projectPath))

        val gradleArguments = System.getProperty("kotlin.test.gradle.import.arguments")
        ExternalSystemUtil.refreshProjects(
            ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                .forceWhenUptodate()
                .useDefaultCallback()
                .use(ProgressExecutionMode.MODAL_SYNC)
                .also {
                    gradleArguments?.run(it::withArguments)
                }
        )

        dispatchAllInvocationEvents()
        ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(false)
        dispatchAllInvocationEvents()

        // WARNING: [VD] DO NOT SAVE PROJECT AS IT COULD PERSIST WRONG MODULES INFO

//        runInEdtAndWait {
//            PlatformTestUtil.saveProject(project)
//        }
    }

    /**
     * @param lookupElements perform basic autocompletion and check presence of suggestion if elements are not empty
     */
    fun typeAndCheckLookup(
        project: Project,
        fileName: String,
        marker: String,
        insertString: String,
        surroundItems: String = "\n",
        lookupElements: List<String>,
        revertChangesAtTheEnd: Boolean = true
    ) {
        val fileInEditor = openFileInEditor(project, fileName)
        val virtualFile = fileInEditor.psiFile.virtualFile
        val editor = EditorFactory.getInstance().getEditors(fileInEditor.document, project)[0]
        val fixture = Fixture(project, editor, virtualFile)

        val initialText = editor.document.text
        try {
            if (isAKotlinScriptFile(fileName)) {
                ScriptDependenciesManager.updateScriptDependenciesSynchronously(virtualFile, project)
            }

            val tasksIdx = fileInEditor.document.text.indexOf(marker)
            assertTrue(tasksIdx > 0)
            editor.caretModel.moveToOffset(tasksIdx + marker.length + 1)

            for (surroundItem in surroundItems) {
                EditorTestUtil.performTypingAction(editor, surroundItem)
            }

            editor.caretModel.moveToOffset(editor.caretModel.offset - 1)
            fixture.type(insertString)

            if (lookupElements.isNotEmpty()) {
                val elements = fixture.complete()
                val items = elements.map { it.lookupString }.toList()
                for (lookupElement in lookupElements) {
                    assertTrue("'$lookupElement' has to be present in items", items.contains(lookupElement))
                }
            }
        } finally {
            // TODO: [VD] revert ?
            //fixture.performEditorAction(IdeActions.SELECTED_CHANGES_ROLLBACK)
            if (revertChangesAtTheEnd) {
                runWriteAction {
                    editor.document.setText(initialText)
                    commitDocument(project, editor.document)
                }
            }
        }
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
        myProject!!, stats, fileName, marker, insertString, surroundItems,
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
        stats.perfTest<Pair<String, Fixture>, Array<LookupElement>>(
            warmUpIterations = 8,
            iterations = 15,
            testName = "typeAndAutocomplete ${notePrefix(note)}$fileName",
            setUp = {
                val fileInEditor = openFileInEditor(project, fileName)
                val file = fileInEditor.psiFile
                val virtualFile = file.virtualFile
                val editor = EditorFactory.getInstance().getEditors(fileInEditor.document, project)[0]
                val fixture = Fixture(project, editor, virtualFile)
                val initialText = editor.document.text
                if (isAKotlinScriptFile(fileName)) {
                    runAndMeasure("update script dependencies for $fileName") {
                        ScriptDependenciesManager.updateScriptDependenciesSynchronously(virtualFile, project)
                    }
                }

                val tasksIdx = fileInEditor.document.text.indexOf(marker)
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
            },
            test = {
                val fixture = it.setUpValue!!.second
                it.value = fixture.complete()
            },
            tearDown = {
                val items = it.value?.map { e -> e.lookupString }?.toList() ?: emptyList()
                try {
                    for (lookupElement in lookupElements) {
                        assertTrue("'$lookupElement' has to be present in items", items.contains(lookupElement))
                    }
                } finally {
                    it.setUpValue?.let { pair ->
                        val document = pair.second.document
                        val file = pair.second.vFile
                        val text = pair.first

                        try {
                            if (revertChangesAtTheEnd) {
                                runWriteAction {
                                    // TODO: [VD] revert ?
                                    //editorFixture.performEditorAction(IdeActions.SELECTED_CHANGES_ROLLBACK)
                                    document.setText(text)
                                    saveDocument(document)
                                    commitDocument(project, document)
                                }
                                dispatchAllInvocationEvents()
                            }
                        } finally {
                            cleanupCaches(project, file)
                        }
                    }
                    commitAllDocuments()
                }
            }
        )
    }

    protected fun enableAnnotatorsAndLoadDefinitions() = enableAnnotatorsAndLoadDefinitions(myProject!!)

    protected fun enableAnnotatorsAndLoadDefinitions(project: Project) {
        ReferenceProvidersRegistry.getInstance() // pre-load tons of classes
        InjectedLanguageManager.getInstance(project) // zillion of Dom Sem classes
        with(LanguageAnnotators.INSTANCE) {
            allForLanguage(JavaLanguage.INSTANCE) // pile of annotator classes loads
            allForLanguage(StdLanguages.XML)
            allForLanguage(KotlinLanguage.INSTANCE)
        }
        DaemonAnalyzerTestCase.assertTrue(
            "side effect: to load extensions",
            ProblemHighlightFilter.EP_NAME.extensions.toMutableList()
                .plus(ImplicitUsageProvider.EP_NAME.extensions)
                .plus(XmlSchemaProvider.EP_NAME.extensions)
                .plus(XmlFileNSInfoProvider.EP_NAME.extensions)
                .plus(ExternalAnnotatorsFilter.EXTENSION_POINT_NAME.extensions)
                .plus(IndexPatternBuilder.EP_NAME.extensions).isNotEmpty()
        )

        // side effect: to load script definitions"
        val scriptDefinitionsManager = ScriptDefinitionsManager.getInstance(project)
        scriptDefinitionsManager.getAllDefinitions()
        dispatchAllInvocationEvents()

        assertTrue(scriptDefinitionsManager.isReady())
        assertFalse(KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled)
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
        perfHighlightFile(myProject!!, name, stats)

    protected fun perfHighlightFile(
        project: Project,
        fileName: String,
        stats: Stats,
        note: String = ""
    ): List<HighlightInfo> {
        return highlightFile {
            val isWarmUp = note == "warm-up"
            var highlightInfos: List<HighlightInfo> = emptyList()
            stats.perfTest<EditorFile, List<HighlightInfo>>(
                warmUpIterations = if (isWarmUp) 1 else 3,
                iterations = if (isWarmUp) 2 else 10,
                testName = "highlighting ${notePrefix(note)}${simpleFilename(fileName)}",
                setUp = {
                    it.setUpValue = openFileInEditor(project, fileName)
                },
                test = {
                    val file = it.setUpValue
                    it.value = highlightFile(project, file!!.psiFile)
                },
                tearDown = {
                    highlightInfos = it.value ?: emptyList()
                    commitAllDocuments()
                    FileEditorManager.getInstance(project).closeFile(it.setUpValue!!.psiFile.virtualFile)
                    PsiManager.getInstance(project).dropPsiCaches()
                }
            )
            highlightInfos
        }
    }

    fun highlightFile(psiFile: PsiFile): List<HighlightInfo> {
        return highlightFile {
            highlightFile(myProject!!, psiFile)
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
        perfScriptDependencies(myProject!!, name, stats, note = note)

    private fun perfScriptDependencies(
        project: Project,
        fileName: String,
        stats: Stats,
        note: String = ""
    ) {
        if (!isAKotlinScriptFile(fileName)) return
        stats.perfTest<EditorFile, EditorFile>(
            testName = "updateScriptDependencies ${notePrefix(note)}${simpleFilename(fileName)}",
            setUp = { it.setUpValue = openFileInEditor(project, fileName) },
            test = {
                ScriptDependenciesManager.updateScriptDependenciesSynchronously(it.setUpValue!!.psiFile.virtualFile, project)
                it.value = it.setUpValue
            },
            tearDown = {
                it.setUpValue?.let { ef -> cleanupCaches(project, ef.psiFile.virtualFile) }
                it.value?.let { v -> assertNotNull(v) }
            }
        )
    }

    fun notePrefix(note: String) = if (note.isNotEmpty()) {
        if (note.endsWith("/")) note else "$note "
    } else ""

    // quite simple impl - good so far
    fun isAKotlinScriptFile(fileName: String) = fileName.endsWith(".kts")

    fun cleanupCaches(project: Project, vFile: VirtualFile) {
        commitAllDocuments()
        FileEditorManager.getInstance(project).closeFile(vFile)
        PsiManager.getInstance(project).dropPsiCaches()
    }

    fun openFileInEditor(project: Project, name: String): EditorFile {
        val fileDocumentManager = FileDocumentManager.getInstance()
        val fileEditorManager = FileEditorManager.getInstance(project)

        val psiFile = projectFileByName(project, name)
        val vFile = psiFile.virtualFile

        assertTrue("file $vFile is not indexed yet", FileIndexFacade.getInstance(project).isInContent(vFile))

        runInEdtAndWait {
            fileEditorManager.openFile(vFile, true)
        }
        val document = fileDocumentManager.getDocument(vFile)!!

        assertNotNull("doc not found for $vFile", EditorFactory.getInstance().getEditors(document))
        assertTrue("expected non empty doc", document.text.isNotEmpty())

        val offset = psiFile.textOffset
        assertTrue("side effect: to load the text", offset >= 0)

        waitForAllEditorsFinallyLoaded(project)

        return EditorFile(psiFile = psiFile, document = document)
    }

    private fun baseName(name: String): String {
        val index = name.lastIndexOf("/")
        return if (index > 0) name.substring(index + 1) else name
    }

    private fun projectFileByName(project: Project, name: String): PsiFile {
        val fileManager = VirtualFileManager.getInstance()
        val url = "${project.guessProjectDir()}/$name"
        val virtualFile = fileManager.refreshAndFindFileByUrl(url)
        if (virtualFile != null) {
            return virtualFile!!.toPsiFile(project)!!
        }

        val baseFileName = baseName(name)
        val projectBaseName = baseName(project.name)

        val virtualFiles = FilenameIndex.getVirtualFilesByName(
            project,
            baseFileName, true,
            GlobalSearchScope.projectScope(project)
        )
            .filter { it.canonicalPath?.contains("/$projectBaseName/$name") ?: false }.toList()

        assertEquals(
            "expected the only file with name '$name'\n, it were: [${virtualFiles.map { it.canonicalPath }.joinToString("\n")}]",
            1,
            virtualFiles.size
        )
        return virtualFiles.iterator().next().toPsiFile(project)!!
    }

    data class EditorFile(val psiFile: PsiFile, val document: Document)

}