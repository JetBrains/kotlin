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

import com.intellij.codeInspection.CommonProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.QuickFix
import com.intellij.codeInspection.reference.RefEntity
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
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.inspections.runInspection
import org.jetbrains.kotlin.idea.maven.inspections.KotlinMavenPluginPhaseInspection
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
        val problemElements = runInspection(inspectionClass, myProject).problemElements
        val actual = problemElements
                .keys()
                .filter { it.name == "pom.xml" }
                .map { problemElements.get(it) }
                .flatMap { it.toList() }
                .mapNotNull { it as? ProblemDescriptorBase }
                .map { SimplifiedProblemDescription(it.descriptionTemplate, it.psiElement.text.replace("\\s+".toRegex(), "")) to it }
                .sortedBy { it.first.text }

        assertEquals(expected.sortedBy { it.text }, actual.map { it.first })

        val suggestedFixes = actual.flatMap { p -> p.second.fixes?.sortedBy { it.familyName }?.map { p.second to it } ?: emptyList() }

        val filenamePrefix = pomFile.nameWithoutExtension + ".fixed."
        val fixFiles = pomFile.parentFile.listFiles { _, name -> name.startsWith(filenamePrefix) && name.endsWith(".xml") }.sortedBy { it.name }

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
            javaFile.viewProvider.document!!.setText("class Test {}\n")
        }

        assertTrue(FileTypeIndex.containsFileOfType(JavaFileType.INSTANCE, myProject.allModules().single().moduleScope))
    }

    private data class SimplifiedProblemDescription(val text: String, val elementText: String)
}