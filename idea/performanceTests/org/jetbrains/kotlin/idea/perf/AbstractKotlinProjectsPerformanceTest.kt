/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.idea.IdeaTestApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.intellij.util.ThrowableRunnable
import com.intellij.util.io.exists
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths

abstract class AbstractKotlinProjectsPerformanceTest : UsefulTestCase() {

    // myProject is not required for all potential perf test cases
    private var myProject: Project? = null
    private lateinit var jdk18: Sdk
    private lateinit var myApplication: IdeaTestApplication

    companion object {
        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val stats: Stats = Stats("-perf")

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            stats.close()
        }
    }

    override fun setUp() {
        super.setUp()

        myApplication = IdeaTestApplication.getInstance()
        ApplicationManager.getApplication().runWriteAction {
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
        InspectionProfileImpl.INIT_INSPECTIONS = true

        // warm up: open simple small project
        if (!warmedUp) {
            val project = innerPerfOpenProject("helloKotlin", "warm-up ")
            val perfHighlightFile = perfHighlightFile(project, "src/HelloMain.kt", "warm-up ")
            assertTrue("kotlin project has been not imported properly", perfHighlightFile.isNotEmpty())
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            ProjectManagerEx.getInstanceEx().closeAndDispose(project)

            warmedUp = true
        }
    }

    override fun tearDown() {
        var runAll = RunAll()

        if (myProject != null) {
            PsiDocumentManager.getInstance(myProject!!).commitAllDocuments()
            runAll = runAll
                .append(ThrowableRunnable { LightPlatformTestCase.doTearDown(myProject!!, myApplication) })
        }

        runAll.append(ThrowableRunnable { super.tearDown() })
            .run()
    }

    private fun getTempDirFixture(): TempDirTestFixture =
        LightTempDirTestFixtureImpl(true)

    protected fun perfChangeDocument(fileName: String, note: String = "", block: (document: Document) -> Unit) =
        perfChangeDocument(myProject!!, fileName, note, block)

    private fun perfChangeDocument(
        project: Project,
        fileName: String,
        nameOfChange: String,
        block: (document: Document) -> Unit
    ) {
        val openFileInEditor = openFileInEditor(project, fileName)
        val document = openFileInEditor.document
        val manager = PsiDocumentManager.getInstance(project)
        CommandProcessor.getInstance().executeCommand(project, {
            ApplicationManager.getApplication().runWriteAction {
                tcSimplePerfTest(fileName, "Changing doc $nameOfChange", stats) {
                    block(document)

                    manager.commitDocument(document)
                }
            }
        }, "change doc $fileName $nameOfChange", "")

        manager.commitAllDocuments()
    }

    protected fun perfOpenProject(name: String, path: String = "idea/testData/perfTest") {
        myProject = innerPerfOpenProject(name, path = path, note = "")
    }

    private fun innerPerfOpenProject(
        name: String,
        note: String,
        path: String = "idea/testData/perfTest"
    ): Project {
        lateinit var project: Project
        val projectPath = "$path/$name"

        tcSimplePerfTest("", "Project ${note}opening $name", stats) {
            project = ProjectManager.getInstance().loadAndOpenProject(projectPath)!!
            if (!Paths.get(projectPath, ".idea").exists()) {
                initKotlinProject(project, projectPath, name)
            }

            ProjectManagerEx.getInstanceEx().openTestProject(project)

            disposeOnTearDown(Disposable { ProjectManagerEx.getInstanceEx().closeAndDispose(project) })
        }

        val changeListManagerImpl = ChangeListManager.getInstance(project) as ChangeListManagerImpl
        changeListManagerImpl.waitUntilRefreshed()

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        return project
    }

    private fun initKotlinProject(
        project: Project,
        projectPath: String,
        name: String
    ) {
        val modulePath = "$projectPath/$name${ModuleFileType.DOT_DEFAULT_EXTENSION}"
        val projectFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectPath))!!
        val srcFile = projectFile.findChild("src")!!
        val module = ApplicationManager.getApplication().runWriteAction(Computable<Module> {
            val projectRootManager = ProjectRootManager.getInstance(project)
            with(projectRootManager) {
                projectSdk = jdk18
            }
            val moduleManager = ModuleManager.getInstance(project)
            val module = moduleManager.newModule(modulePath, ModuleTypeId.JAVA_MODULE)
            PsiTestUtil.addSourceRoot(module, srcFile)
            module!!
        })
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, jdk18)
    }

    protected fun perfHighlightFile(name: String): List<HighlightInfo> =
        perfHighlightFile(myProject!!, name)


    private fun perfHighlightFile(
        project: Project,
        name: String,
        note: String = ""
    ): List<HighlightInfo> {
        val fileInEditor = openFileInEditor(project, name)
        val file = fileInEditor.psiFile

        var highlightFile: List<HighlightInfo> = emptyList()
        tcSimplePerfTest(file.name, "Highlighting file $note${file.name}", stats) {
            highlightFile = highlightFile(file)
        }
        return highlightFile
    }

    fun perfAutoCompletion(
        name: String,
        before: String,
        suggestions: Array<String>,
        type: String,
        after: String
    ) {
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE)
        val tempDirFixture = getTempDirFixture()
        val fixture = factory.createCodeInsightFixture(fixtureBuilder.fixture, tempDirFixture)

        with(fixture) {
            setUp()
            configureByText(KotlinFileType.INSTANCE, before)
        }

        var complete: Array<LookupElement>? = null
        tcSimplePerfTest("", "Auto completion $name", stats) {
            with(fixture) {
                complete = complete(CompletionType.BASIC)
            }
        }

        val actualSuggestions = complete?.map { it.lookupString }?.toList() ?: emptyList()
        assertTrue(actualSuggestions.containsAll(suggestions.toList()))

        try {
            with(fixture) {
                type(type)
                checkResult(after)
            }
        } finally {
            PsiDocumentManager.getInstance(fixture.project).commitAllDocuments()
            fixture.tearDown()
        }
    }

    private fun highlightFile(psiFile: PsiFile): List<HighlightInfo> {
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile)!!
        val editor = EditorFactory.getInstance().getEditors(document).first()
        return CodeInsightTestFixtureImpl.instantiateAndRun(psiFile, editor, intArrayOf(), false)
    }

    data class EditorFile(val psiFile: PsiFile, val document: Document)

    private fun openFileInEditor(project: Project, name: String): EditorFile {
        val psiFile = projectFileByName(project, name)
        val vFile = psiFile.virtualFile
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openFile(vFile, true)
        val document = FileDocumentManager.getInstance().getDocument(vFile)!!
        assertNotNull(EditorFactory.getInstance().getEditors(document))
        disposeOnTearDown(Disposable { fileEditorManager.closeFile(vFile) })
        return EditorFile(psiFile = psiFile, document = document)
    }

    private fun projectFileByName(project: Project, name: String): PsiFile {
        val fileManager = VirtualFileManager.getInstance()
        val url = "file://${File("${project.basePath}/$name").absolutePath}"
        val virtualFile = fileManager.refreshAndFindFileByUrl(url)
        return virtualFile!!.toPsiFile(project)!!
    }
}