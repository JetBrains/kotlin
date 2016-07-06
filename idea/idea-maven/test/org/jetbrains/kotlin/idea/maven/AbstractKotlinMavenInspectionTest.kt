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

package org.jetbrains.kotlin.idea.maven

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.maven.inspections.KotlinMavenPluginPhaseInspection
import org.jetbrains.kotlin.idea.maven.inspections.DifferentKotlinMavenVersionInspection
import org.jetbrains.kotlin.idea.refactoring.toPsiDirectory
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File

abstract class AbstractKotlinMavenInspectionTest : MavenImportingTestCase() {

    override fun setUp() {
        super.setUp()
        repositoryPath = File(myDir, "repo").path
        createStdProjectFolders()
    }

    fun doTest(fileName: String) {
        val pomFile = File(fileName)
        val pomText = pomFile.readText()

        createPomFile(fileName)
        importProject()
        myProject.allModules().forEach {
            setupJdkForModule(it.name)
        }

        if (pomText.contains("<!--\\s*mkjava\\s*-->".toRegex(RegexOption.MULTILINE))) {
            mkJavaFile()
        }

        val inspectionClassName = "<!--\\s*inspection:\\s*([\\S]+)\\s-->".toRegex().find(pomText)?.groups?.get(1)?.value ?: KotlinMavenPluginPhaseInspection::class.qualifiedName !!
        val inspectionClass = Class.forName(inspectionClassName)

        val matcher = "<!--\\s*problem:\\s*on\\s*([^,]+),\\s*title\\s*(.+)\\s*-->".toRegex()
        val expected = pomText.lines().mapNotNull { matcher.find(it) }.map { SimplifiedProblemDescription(it.groups[2]!!.value.trim(), it.groups[1]!!.value.trim()) }
        val actual = runInspection(inspectionClass).sortedBy { it.first.text }

        assertEquals(expected.sortedBy { it.text }, actual.map { it.first })

        val suggestedFixes = actual.flatMap { p -> p.second.fixes?.sortedBy { it.familyName }?.map { p.second to it } ?: emptyList() }

        val filenamePrefix = pomFile.nameWithoutExtension + ".fixed."
        val fixFiles = pomFile.parentFile.listFiles { file, name -> name.startsWith(filenamePrefix) && name.endsWith(".xml") }.sortedBy { it.name }

        if (fixFiles.size > suggestedFixes.size) {
            fail("Not all fixes were suggested by the inspection: expected count: ${fixFiles.size}, actual fixes count: ${suggestedFixes.size}")
        }
        if (fixFiles.size < suggestedFixes.size) {
            fail("Not all fixes covered by *.fixed.N.xml files")
        }

        val documentManager = PsiDocumentManager.getInstance(myProject)
        val document = documentManager.getDocument(PsiManager.getInstance(myProject).findFile(myProjectPom)!!)!!
        val originalText = document.text

        fixFiles.forEachIndexed { index, file ->
            val (problem, quickfix) = suggestedFixes[index]

            quickfix.applyFix(problem)

            assertEquals(FileUtil.loadFile(file, true).trim(), document.text.trim())

            ApplicationManager.getApplication().runWriteAction {
                document.setText(originalText)
                documentManager.commitDocument(document)
            }
        }
    }

    private fun createPomFile(fileName: String) {
        myProjectPom = myProjectRoot.findChild("pom.xml")
        if (myProjectPom == null) {
            myProjectPom = object : WriteAction<VirtualFile>() {
                override fun run(result: Result<VirtualFile>) {
                    val res = myProjectRoot.createChildData(null, "pom.xml")
                    result.setResult(res)
                }
            }.execute().resultObject
        }
        myAllPoms.add(myProjectPom!!)

        ApplicationManager.getApplication().runWriteAction {
            myProjectPom!!.setBinaryContent(File(fileName).readBytes())
        }
    }

    private fun QuickFix<CommonProblemDescriptor>.applyFix(desc: ProblemDescriptorBase) {
        CommandProcessor.getInstance().executeCommand(myProject, {
            ApplicationManager.getApplication().runWriteAction {
                applyFix(myProject, desc)

                val manager = PsiDocumentManager.getInstance(myProject)
                val document = manager.getDocument(PsiManager.getInstance(myProject).findFile(myProjectPom)!!)!!
                manager.doPostponedOperationsAndUnblockDocument(document)
                manager.commitDocument(document)
                FileDocumentManager.getInstance().saveDocument(document)

            }

            println(myProjectPom.contentsToByteArray().toString(Charsets.UTF_8))
        }, "quick-fix-$name", "Kotlin")
    }

    private fun mkJavaFile() {
        val sourceFolder = getContentRoots(myProject.allModules().single().name).single().getSourceFolders(JavaSourceRootType.SOURCE).single()
        ApplicationManager.getApplication().runWriteAction {
            val javaFile = sourceFolder.file?.toPsiDirectory(myProject)?.createFile("Test.java") ?: throw IllegalStateException()
            javaFile.virtualFile.setBinaryContent("class Test {}\n".toByteArray())

            FileBasedIndex.getInstance().ensureUpToDate(FileTypeIndex.NAME, myProject, GlobalSearchScope.projectScope(myProject))
            myProject.allModules().forEach { module ->
                FileBasedIndex.getInstance().ensureUpToDate(FileTypeIndex.NAME, myProject, module.moduleScope)
            }
        }

        assertTrue(FileTypeIndex.containsFileOfType(JavaFileType.INSTANCE, myProject.allModules().single().moduleScope))
    }

    private fun runInspection(inspectionClass: Class<*>): List<Pair<SimplifiedProblemDescription, ProblemDescriptorBase>> {
        val toolWrapper = LocalInspectionToolWrapper(inspectionClass.newInstance() as LocalInspectionTool)

        val tool = toolWrapper.tool
        if (tool is DifferentKotlinMavenVersionInspection) {
            tool.testVersionMessage = "\$PLUGIN_VERSION"
        }

        val scope = AnalysisScope(myProject)
        val inspectionManager = (InspectionManager.getInstance(myProject) as InspectionManagerEx)
        val globalContext = CodeInsightTestFixtureImpl.createGlobalContextForTool(scope, myProject, inspectionManager, toolWrapper)

        InspectionTestUtil.runTool(toolWrapper, scope, globalContext)
        val presentation = globalContext.getPresentation(toolWrapper)

        return presentation.problemElements.filter { it.key.name == "pom.xml" }
                .values
                .flatMap { it.toList() }
                .mapNotNull { it as? ProblemDescriptorBase }
                .map { SimplifiedProblemDescription(it.descriptionTemplate, it.psiElement.text.replace("\\s+".toRegex(), "")) to it }
    }

    private data class SimplifiedProblemDescription(val text: String, val elementText: String)
}