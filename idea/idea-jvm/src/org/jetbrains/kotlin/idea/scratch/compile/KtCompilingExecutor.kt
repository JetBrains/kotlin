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

package org.jetbrains.kotlin.idea.scratch.compile

import com.intellij.execution.process.ProcessOutput
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.scratch.*
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutput
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputType
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.AnalyzingUtils

class KtCompilingExecutor(file: ScratchFile) : ScratchExecutor(file) {
    private var session: KtScratchExecutionSession? = null

    override fun execute() {
        handler.clear(file)
        handler.onStart(file)

        session = KtScratchExecutionSession(file, this)
        session?.execute {
            handler.onFinish(file)
            session = null
        }
    }

    override fun stop() {
        if (session == null) return

        try {
            session?.stop()
        } finally {
            handler.onFinish(file)
        }
    }

    fun checkForErrors(psiFile: KtFile, expressions: List<ScratchExpression>): Boolean {
        return runReadAction {
            try {
                AnalyzingUtils.checkForSyntacticErrors(psiFile)
            } catch (e: IllegalArgumentException) {
                errorOccurs(e.message ?: "Couldn't compile ${psiFile.name}", isFatal = true)
                return@runReadAction false
            }

            val analysisResult = psiFile.analyzeWithAllCompilerChecks()

            if (analysisResult.isError()) {
                errorOccurs(analysisResult.error.message ?: "Couldn't compile ${psiFile.name}", isFatal = true)
                return@runReadAction false
            }

            val bindingContext = analysisResult.bindingContext
            val diagnostics = bindingContext.diagnostics.filter { it.severity == Severity.ERROR }
            if (diagnostics.isNotEmpty()) {
                val scratchPsiFile = file.getPsiFile()
                diagnostics.forEach { diagnostic ->
                    val errorText = DefaultErrorMessages.render(diagnostic)
                    if (psiFile == scratchPsiFile) {
                        if (diagnostic.psiElement.containingFile == psiFile) {
                            val scratchExpression = expressions.findExpression(diagnostic.psiElement)
                            if (scratchExpression == null) {
                                LOG.error("Couldn't find expression to report error: ${diagnostic.psiElement.getElementTextWithContext()}")
                                handler.error(file, errorText)

                            } else {
                                handler.handle(file, scratchExpression, ScratchOutput(errorText, ScratchOutputType.ERROR))
                            }
                        } else {
                            handler.error(file, errorText)
                        }
                    } else {
                        handler.error(file, errorText)
                    }
                }
                handler.onFinish(file)
                return@runReadAction false
            }
            return@runReadAction true
        }
    }

    fun parseOutput(processOutput: ProcessOutput, expressions: List<ScratchExpression>) {
        ProcessOutputParser(expressions).parse(processOutput)
    }

    private fun List<ScratchExpression>.findExpression(psiElement: PsiElement): ScratchExpression? {
        val elementLine = psiElement.getLineNumber()
        return runReadAction { firstOrNull { elementLine in it.lineStart..it.lineEnd } }
    }

    private fun List<ScratchExpression>.findExpression(lineStart: Int, lineEnd: Int): ScratchExpression? {
        return runReadAction { firstOrNull { it.lineStart == lineStart && it.lineEnd == lineEnd } }
    }

    private inner class ProcessOutputParser(private val expressions: List<ScratchExpression>) {
        fun parse(processOutput: ProcessOutput) {
            val out = processOutput.stdout
            val err = processOutput.stderr
            if (err.isNotBlank()) {
                handler.error(file, err)
            }
            if (out.isNotBlank()) {
                parseStdOut(out)
            }
        }

        private fun parseStdOut(out: String) {
            var results = arrayListOf<String>()
            var userOutput = arrayListOf<String>()
            for (line in out.split("\n")) {
                LOG.printDebugMessage("Compiling executor output: $line")

                if (isOutputEnd(line)) {
                    return
                }

                if (isGeneratedOutput(line)) {
                    val lineWoPrefix = line.removePrefix(KtScratchSourceFileProcessor.GENERATED_OUTPUT_PREFIX)
                    if (isResultEnd(lineWoPrefix)) {
                        val extractedLineInfo = extractLineInfoFrom(lineWoPrefix)
                            ?: return errorOccurs("Couldn't extract line info from line: $lineWoPrefix", isFatal = true)
                        val (startLine, endLine) = extractedLineInfo
                        val scratchExpression = expressions.findExpression(startLine, endLine)
                        if (scratchExpression == null) {
                            LOG.error(
                                "Couldn't find expression with start line = $startLine, end line = $endLine.\n" +
                                        expressions.joinToString("\n")
                            )
                        } else {
                            userOutput.forEach { output ->
                                handler.handle(file, scratchExpression, ScratchOutput(output, ScratchOutputType.OUTPUT))
                            }

                            results.forEach { result ->
                                handler.handle(file, scratchExpression, ScratchOutput(result, ScratchOutputType.RESULT))
                            }
                        }

                        results = arrayListOf()
                        userOutput = arrayListOf()
                    } else if (lineWoPrefix != Unit.toString()) {
                        results.add(lineWoPrefix)
                    }
                } else {
                    userOutput.add(line)
                }
            }
        }

        private fun isOutputEnd(line: String) = line.removeSuffix("\n") == KtScratchSourceFileProcessor.END_OUTPUT_MARKER
        private fun isResultEnd(line: String) = line.startsWith(KtScratchSourceFileProcessor.LINES_INFO_MARKER)
        private fun isGeneratedOutput(line: String) = line.startsWith(KtScratchSourceFileProcessor.GENERATED_OUTPUT_PREFIX)

        private fun extractLineInfoFrom(encoded: String): Pair<Int, Int>? {
            val lineInfo = encoded
                .removePrefix(KtScratchSourceFileProcessor.LINES_INFO_MARKER)
                .removeSuffix("\n")
                .split('|')
            if (lineInfo.size == 2) {
                try {
                    val (a, b) = lineInfo[0].toInt() to lineInfo[1].toInt()
                    if (a > -1 && b > -1) {
                        return a to b
                    }
                } catch (e: NumberFormatException) {
                }
            }
            return null
        }
    }
}