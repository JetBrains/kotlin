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
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.inspections.runInspection
import org.jetbrains.kotlin.idea.maven.inspections.KotlinMavenPluginPhaseInspection
import org.jetbrains.kotlin.idea.refactoring.toPsiDirectory
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.keysToMap
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

        val inspectionClassName = "<!--\\s*inspection:\\s*([\\S]+)\\s-->".toRegex().find(pomText)?.groups?.get(1)?.value
                ?: KotlinMavenPluginPhaseInspection::class.qualifiedName!!
        val inspectionClass = Class.forName(inspectionClassName)

        val matcher = "<!--\\s*problem:\\s*on\\s*([^,]+),\\s*title\\s*(.+)\\s*-->".toRegex()
        val expectedProblemsText = pomText.lines()
            .filter { matcher.matches(it) }
            .joinToString("\n")

        val problemElements = runInspection(inspectionClass, myProject).problemElements
        val actualProblems = problemElements
                .filter { it.key.name == "pom.xml" }
                .values
                .flatMap { it.toList() }
                .mapNotNull { it as? ProblemDescriptorBase }

        val actual = actualProblems
            .map { SimplifiedProblemDescription(it.descriptionTemplate, it.psiElement.text.replace("\\s+".toRegex(), "")) to it }
            .sortedBy { it.first.text }

        val actualProblemsText = actual
            .map { it.first }
            .joinToString("\n") { "<!-- problem: on ${it.elementText}, title ${it.text} -->"}

        assertEquals(expectedProblemsText, actualProblemsText)

        val suggestedFixes = actual.flatMap { p -> p.second.fixes?.sortedBy { it.familyName }?.map { p.second to it } ?: emptyList() }

        val filenamePrefix = pomFile.nameWithoutExtension + ".fixed."
        val fixFiles =
            pomFile.parentFile.listFiles { _, name -> name.startsWith(filenamePrefix) && name.endsWith(".xml") }.sortedBy { it.name }

        val rangesToFixFiles: Map<File, IntRange> = fixFiles.keysToMap {
            val fixFileName = it.name
            val fixRangeStr = fixFileName.substringBeforeLast('.').substringAfterLast('.')
            val numbers = fixRangeStr.split('-').map { it.toInt() }
            when (numbers.size) {
                0 -> error("No number in fix file $fixFileName")
                1 -> IntRange(numbers[0], numbers[0])
                2 -> IntRange(numbers[0], numbers[1])
                else -> error("Bad range `$fixRangeStr` in fix file $fixFileName")
            }
        }

        val sortedFixRanges = rangesToFixFiles.values.sortedBy { it.start }
        sortedFixRanges.forEachIndexed { i, range ->
            if (i > 0) {
                val previous = sortedFixRanges[i - 1]
                if (previous.endInclusive + 1 != range.start) {
                    error("Bad ranges in fix files: $previous and $range")
                }
            }
        }

        val numberOfFixDataFiles = sortedFixRanges.lastOrNull()?.endInclusive ?: 0
        if (numberOfFixDataFiles > suggestedFixes.size) {
            fail("Not all fixes were suggested by the inspection: expected count: ${fixFiles.size}, actual fixes count: ${suggestedFixes.size}")
        }
        if (numberOfFixDataFiles < suggestedFixes.size) {
            fail("Not all fixes covered by *.fixed.N.xml files")
        }

        val documentManager = PsiDocumentManager.getInstance(myProject)
        val document = documentManager.getDocument(PsiManager.getInstance(myProject).findFile(myProjectPom)!!)!!
        val originalText = document.text

        suggestedFixes.forEachIndexed { index, suggestedFix ->
            val (problem, quickfix) = suggestedFix
            val file = rangesToFixFiles.entries.first { (_, range) -> index + 1 in range }.key

            quickfix.applyFix(problem)

            KotlinTestUtils.assertEqualsToFile(file, document.text.trim())

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
        val sourceFolder =
            getContentRoots(myProject.allModules().single().name).single().getSourceFolders(JavaSourceRootType.SOURCE).single()
        ApplicationManager.getApplication().runWriteAction {
            val javaFile = sourceFolder.file?.toPsiDirectory(myProject)?.createFile("Test.java") ?: throw IllegalStateException()
            javaFile.viewProvider.document!!.setText("class Test {}\n")
        }

        assertTrue(FileTypeIndex.containsFileOfType(JavaFileType.INSTANCE, myProject.allModules().single().moduleScope))
    }

    private data class SimplifiedProblemDescription(val text: String, val elementText: String)
}