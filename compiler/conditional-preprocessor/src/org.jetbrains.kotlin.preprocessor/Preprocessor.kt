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

package org.jetbrains.kotlin.preprocessor

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    require(args.size() == 1, "Please specify path to sources")

    val sourcePath = File(args.first())

//    val evaluators = listOf(JvmPlatformEvaluator(version = 7), JsPlatformEvaluator())

    //println("Using condition evaluator: $evaluators")

    val targetPath = File("libraries/stdlib/target")


    val profiles = listOf(6, 7, 8).map { Preprocessor(createJvmProfile(targetPath, version = it)) }

    val pool = Executors.newFixedThreadPool(4)

    profiles.forEach { pool.submit { it.processSources(sourcePath) } }

    pool.shutdown()
    pool.awaitTermination(1, TimeUnit.MINUTES)
}


data class Modification(val range: TextRange, val selector: (String) -> String)

class CollectModificationsVisitor(evaluators: List<Evaluator>) : JetTreeVisitorVoid() {

    val elementModifications: Map<Evaluator, MutableList<Modification>> =
            evaluators.toMap(selector = { it }, transform = { arrayListOf<Modification>() })

    override fun visitDeclaration(declaration: JetDeclaration) {
        super.visitDeclaration(declaration)

        val annotations = declaration.parseConditionalAnnotations()
        val name = (declaration as? JetNamedDeclaration)?.nameAsSafeName ?: declaration.name

        val declResults = arrayListOf<Pair<Evaluator, Boolean>>()
        for ((evaluator, modifications) in elementModifications) {
            val conditionalResult = evaluator(annotations)
            declResults.add(evaluator to conditionalResult)

            if (!conditionalResult)
                modifications.add(Modification(declaration.textRange) {""})
            else {
                val targetName = annotations.filterIsInstance<Conditional.TargetName>().singleOrNull()
                if (targetName != null) {
                    val placeholderName = (declaration as JetNamedDeclaration).nameAsName!!.asString()
                    val realName = targetName.name
                    modifications.add(Modification(declaration.textRange) { it.replace(placeholderName, realName) })
                }
            }

        }
        //println("declaration: ${declaration.javaClass.simpleName} $name${if (annotations.isNotEmpty()) ", annotations: ${annotations.joinToString { it.toString() }}, evaluation result: $declResults" else ""}")
    }
}


data class Profile(val name: String, val evaluator: Evaluator, val targetRoot: File)

fun createJvmProfile(targetRoot: File, version: Int) = Profile("JVM$version", JvmPlatformEvaluator(version), File(targetRoot, "jvm$version"))
fun createJsProfile(targetRoot: File) = Profile("JS", JsPlatformEvaluator(), File(targetRoot, "js"))


public class Preprocessor(val profile: Profile) {

    val fileType = JetFileType.INSTANCE
    val jetPsiFactory: JetPsiFactory

    init {
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(Disposable {  }, configuration, emptyList())

        val project = environment.project
        jetPsiFactory = JetPsiFactory(project)
    }

    sealed class FileProcessingResult {
        object Skip : FileProcessingResult()
        object Copy : FileProcessingResult()

        class Modify(val sourceText: String, val modifications: List<Modification>) : FileProcessingResult()
    }

    public fun processSources(sourceRoot: File) {
        processDirectorySingleEvaluator(sourceRoot, profile.targetRoot, profile.evaluator)
    }

    private fun processFileSingleEvaluator(sourceFile: File, evaluator: Evaluator): FileProcessingResult {
        if (sourceFile.extension != fileType.defaultExtension)
            return FileProcessingResult.Copy

        val sourceText = sourceFile.readText().convertLineSeparators()
        val psiFile = jetPsiFactory.createFile(sourceFile.name, sourceText)
        println("$psiFile")


        val fileAnnotations = psiFile.parseConditionalAnnotations()
        if (!evaluator(fileAnnotations))
            return FileProcessingResult.Skip


        val visitor = CollectModificationsVisitor(listOf(evaluator))
        psiFile.accept(visitor)

        val list = visitor.elementModifications.values().single()
        return if (list.isNotEmpty())
            FileProcessingResult.Modify(sourceText, list)
        else
            FileProcessingResult.Copy
    }

    private fun processDirectorySingleEvaluator(sourceRoot: File, targetRoot: File, evaluator: Evaluator) {
        val (sourceFiles, sourceDirectories) = sourceRoot.listFiles().partition { !it.isDirectory }

        val processedFiles = hashSetOf<File>()
        for (sourceFile in sourceFiles)
        {
            val result = processFileSingleEvaluator(sourceFile, evaluator)
            if (result is FileProcessingResult.Skip)
                continue

            val destFile = sourceFile.makeRelativeTo(sourceRoot, targetRoot)
            processedFiles += destFile

            if (destFile.exists() && destFile.isDirectory)
                destFile.deleteRecursively()

            // if no modifications — copy
            if (result is FileProcessingResult.Copy) {
                FileUtil.copy(sourceFile, destFile)
            } else if (result is FileProcessingResult.Modify) {
                val resultText = applyModifications(result.modifications, result.sourceText, evaluator)
                if (destFile.exists() && destFile.isTextEqualTo(resultText))
                    continue
                destFile.writeText(resultText)
            }
        }

        for (sourceDir in sourceDirectories) {
            val destDir = sourceDir.makeRelativeTo(sourceRoot, targetRoot)
            if (!destDir.exists()) {
                destDir.mkdirsOrFail()
            }
            else if (!destDir.isDirectory) {
                destDir.delete()
            }
            processDirectorySingleEvaluator(sourceDir, destDir, evaluator)
            processedFiles += destDir
        }

        targetRoot.listFiles().forEach { targetFile ->
            if (!processedFiles.remove(processedFiles.find { FileUtil.filesEqual(it, targetFile) })) {
                targetFile.deleteRecursively()
            }
        }
    }


    private fun processFileMultiEvaluators(sourceFile: File, evaluators: List<Evaluator>): List<FileProcessingResult> {
        if (sourceFile.extension != fileType.defaultExtension)
            return evaluators map { FileProcessingResult.Copy }

        val sourceText = sourceFile.readText().convertLineSeparators()
        val psiFile = jetPsiFactory.createFile(sourceFile.name, sourceText)
        println("$psiFile")

        val results = hashMapOf<Evaluator, FileProcessingResult>()

        val fileAnnotations = psiFile.parseConditionalAnnotations()
        evaluators.forEach { evaluator ->
            if (!evaluator(fileAnnotations))
                results += evaluator to FileProcessingResult.Skip
        }

        val visitor = CollectModificationsVisitor(evaluators - results.keySet())
        psiFile.accept(visitor)

        for ((evaluator, list) in visitor.elementModifications) {
            val result =
                if (list.isNotEmpty())
                    FileProcessingResult.Modify(sourceText, list)
                else
                    FileProcessingResult.Copy
            results += evaluator to result
        }
        return evaluators.map { results[it]!! }
    }

    private fun processDirectoryMultiEvaluators(sourceRoot: File, targetRelativeRoot: File, profiles: List<Profile>) {

        val (sourceFiles, sourceDirectories) = sourceRoot.listFiles().partition { !it.isDirectory }

        // TODO: keep processed file list for each profile
        val processedFiles = profiles.toMap({ it }, { hashSetOf<File>()})

    }

    private fun applyModifications(modifications: List<Modification>, sourceText: String, evaluator: Evaluator): String {
        var prevIndex = 0
        val result = StringBuilder()
        for ((range, selector) in modifications) {
            result.append(sourceText, prevIndex, range.startOffset)
            val rangeText = range.substring(sourceText)
            val newValue = selector(rangeText)
            if (newValue.isEmpty()) {
                result.append("/* Not available on $evaluator */")
                repeat(StringUtil.getLineBreakCount(rangeText)) {
                    result.append("\n")
                }
            }
            else {
                result.append(newValue)
            }
            prevIndex = range.endOffset
        }
        result.append(sourceText, prevIndex, sourceText.length())
        return result.toString()
    }

}

fun String.convertLineSeparators(): String = StringUtil.convertLineSeparators(this)

fun File.isTextEqualTo(content: String): Boolean = readText().lines() == content.lines()

fun File.makeRelativeTo(from: File, to: File) = File(to, relativeTo(from))

fun File.mkdirsOrFail() {
    if (!mkdirs() && !exists()) {
        throw IOException("Failed to create directory $this.")
    }
}