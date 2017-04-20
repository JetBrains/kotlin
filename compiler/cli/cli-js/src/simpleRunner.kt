/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.cli

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import java.io.File


private val EXTENSIONS_DIR = "extensions/"
private val COMMON_CONFIG_FILE = EXTENSIONS_DIR + "common.xml"
private val JS_CONFIG_FILE = EXTENSIONS_DIR + "kotlin2js.xml"

val JS_CONFIG_FILES = listOf(COMMON_CONFIG_FILE, JS_CONFIG_FILE)

fun main(args: Array<String>) {
    fun Array<String>.getParam(name: String, default: String): String {
        val prefix = "-$name="
        return firstOrNull { it.startsWith(prefix) }?.substring(prefix.length) ?: default
    }
    val rootDisposable = Disposer.newDisposable()
    val configuration = CompilerConfiguration()

    configuration.put(CommonConfigurationKeys.MODULE_NAME, "JS_TESTS")
    val libraries = args.getParam("libraries", "")
    val output = args.getParam("output", "")
    val sources = args.filter { !it.startsWith("-") && it.endsWith(".kt") }
    configuration.put(JSConfigurationKeys.LIBRARIES, libraries.split(":").filter(String::isNotEmpty))

    val env = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, JS_CONFIG_FILES)
    val ktFiles = getKtFiles(env.project, sources) {}
    env.addSourceFiles(ktFiles)

    val config = JsConfig(env.project, env.configuration)
    val analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(env.getSourceFiles(), config)

    val (errors, other) = analysisResult.bindingContext.diagnostics.partition { it.severity == Severity.ERROR }

    if (errors.isNotEmpty()) {
        printDiagnosticsToStderr(errors)
        return
    }

    if (reportSyntacticErrors(ktFiles)) return

//    printDiagnosticsToStderr(other)

    val tr = K2JSTranslator(config)
    val result = tr.translate(env.getSourceFiles(), MainCallParameters.noCall(), analysisResult)

    val generatedCode = (result as TranslationResult.Success).program.globalBlock.toString()

    if (output.isNotEmpty()) {
        val outFile = File(output)
        outFile.writeText(generatedCode)
    }
    else {
        System.out.println(generatedCode)
    }
}

private fun printDiagnosticsToStderr(diagnostics: List<Diagnostic>) {
    for (d in diagnostics) {
        if (!d.isValid) continue

        val psiFile = d.psiFile
        val textRanges = d.textRanges
        val diagnosticText = DefaultErrorMessages.render(d)
        System.err.println(d.severity.name + ": " + d.factory.name + ": " + diagnosticText + " " + DiagnosticUtils.atLocation(psiFile, textRanges[0]))
    }
}

fun reportSyntacticErrors(files: List<KtFile>): Boolean {
    var hasError = false
    for (file in files) {
        file.acceptChildren( object : KtTreeVisitorVoid() {
            override fun visitErrorElement(@NotNull element: PsiErrorElement) {
                hasError = true
                System.err.println("ERROR: " + element.errorDescription + "; looking at " + element.node.elementType + " '" + element.text + DiagnosticUtils.atLocation(element))
            }
        })
    }

    return hasError
}

fun getKtFiles(
        project: Project,
        sourceRoots: Collection<String>,
        reportError: (String) -> Unit
): List<KtFile> {
    val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

    val processedFiles = Sets.newHashSet<VirtualFile>()
    val result = Lists.newArrayList<KtFile>()

    val virtualFileCreator = PreprocessedFileCreator(project)

    for (sourceRootPath in sourceRoots) {
        val vFile = localFileSystem.findFileByPath(sourceRootPath)
        if (vFile == null) {
            val message = "Source file or directory not found: " + sourceRootPath
            reportError.invoke(message)
            continue
        }
        if (!vFile.isDirectory && vFile.fileType !== KotlinFileType.INSTANCE) {
            reportError.invoke("Source entry is not a Kotlin file: " + sourceRootPath)
            continue
        }

        File(sourceRootPath).walkTopDown().forEach { file ->
            if (file.isFile) {
                val originalVirtualFile = localFileSystem.findFileByPath(file.absolutePath)
                val virtualFile = if (originalVirtualFile != null) virtualFileCreator.create(originalVirtualFile) else null
                if (virtualFile != null && !processedFiles.contains(virtualFile)) {
                    processedFiles.add(virtualFile)
                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    if (psiFile is KtFile) {
                        result.add(psiFile as KtFile?)
                    }
                }
            }
            Unit
        }
    }

    return result
}