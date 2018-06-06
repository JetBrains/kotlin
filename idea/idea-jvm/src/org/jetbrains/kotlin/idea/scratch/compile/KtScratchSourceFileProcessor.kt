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

import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.psi.*

class KtScratchSourceFileProcessor {
    companion object {
        const val GENERATED_OUTPUT_PREFIX = "##scratch##generated##"
        const val LINES_INFO_MARKER = "end##"
        const val END_OUTPUT_MARKER = "end##!@#%^&*"

        const val OBJECT_NAME = "ScratchFileRunnerGenerated"
        const val INSTANCE_NAME = "instanceScratchFileRunner"
        const val PACKAGE_NAME = "org.jetbrains.kotlin.idea.scratch.generated"
        const val GET_RES_FUN_NAME_PREFIX = "generated_get_instance_res"
    }

    fun process(file: ScratchFile): Result {
        val sourceProcessor = KtSourceProcessor()
        file.getExpressions().forEach {
                sourceProcessor.process(it)
            }

        val codeResult =
            """
                package $PACKAGE_NAME

                ${sourceProcessor.imports.joinToString("\n") { it.text }}

                object $OBJECT_NAME {
                    class $OBJECT_NAME {
                        ${sourceProcessor.classBuilder}
                    }

                    @JvmStatic fun main(args: Array<String>) {
                        val $INSTANCE_NAME = $OBJECT_NAME()
                        ${sourceProcessor.objectBuilder}
                        println("$END_OUTPUT_MARKER")
                    }
                }
            """
        return Result.OK("$PACKAGE_NAME.$OBJECT_NAME", codeResult)
    }

    class KtSourceProcessor {
        val classBuilder = StringBuilder()
        val objectBuilder = StringBuilder()
        val imports = arrayListOf<KtImportDirective>()

        private var resCount = 0

        fun process(expression: ScratchExpression) {
            val psiElement = expression.element
            when (psiElement) {
                is KtVariableDeclaration -> processDeclaration(expression, psiElement)
                is KtFunction -> processDeclaration(expression, psiElement)
                is KtClassOrObject -> processDeclaration(expression, psiElement)
                is KtImportDirective -> imports.add(psiElement)
                is KtExpression -> processExpression(expression, psiElement)
            }
        }

        private fun processDeclaration(e: ScratchExpression, c: KtDeclaration) {
            classBuilder.append(c.text).newLine()

            val descriptor = c.resolveToDescriptorIfAny() ?: return

            val context = RenderingContext.of(descriptor)
            objectBuilder.println(Renderers.COMPACT.render(descriptor, context))
            objectBuilder.appendLineInfo(e)
        }

        private fun processExpression(e: ScratchExpression, expr: KtExpression) {
            val resName = "$GET_RES_FUN_NAME_PREFIX$resCount"

            classBuilder.append("fun $resName() = run { ${expr.text} }").newLine()

            objectBuilder.printlnObj("$INSTANCE_NAME.$resName()")
            objectBuilder.appendLineInfo(e)

            resCount += 1
        }

        private fun StringBuilder.appendLineInfo(e: ScratchExpression) {
            println("$LINES_INFO_MARKER${e.lineStart}|${e.lineEnd}")
        }

        private fun StringBuilder.println(str: String) = append("println(\"$GENERATED_OUTPUT_PREFIX$str\")").newLine()
        private fun StringBuilder.printlnObj(str: String) = append("println(\"$GENERATED_OUTPUT_PREFIX\${$str}\")").newLine()
        private fun StringBuilder.newLine() = append("\n")
    }

    sealed class Result {
        class Error(val message: String) : Result()
        class OK(val mainClassName: String, val code: String) : Result()
    }
}