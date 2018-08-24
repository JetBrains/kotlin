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

package org.jetbrains.kotlin.codegen

import com.google.common.collect.Lists
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.rethrow
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*
import java.util.regex.Pattern

abstract class AbstractLineNumberTest : CodegenTestCase() {

    override fun doMultiFileTest(
        wholeFile: File, files: MutableList<CodegenTestCase.TestFile>, javaFilesDir: File?
    ) {
        val isCustomTest = wholeFile.parentFile.name.equals("custom", ignoreCase = true)
        if (!isCustomTest) {
            files.add(createLineNumberDeclaration())
        }
        compile(files, javaFilesDir)

        val psiFile = myFiles.psiFiles.single { file -> file.name == wholeFile.name }

        try {
            if (isCustomTest) {
                val actualLineNumbers = extractActualLineNumbersFromBytecode(classFileFactory, false)
                val text = psiFile.text
                val newFileText = text.substring(0, text.indexOf("// ")) + getActualLineNumbersAsString(actualLineNumbers)
                KotlinTestUtils.assertEqualsToFile(wholeFile, newFileText)
            } else {
                val expectedLineNumbers = extractSelectedLineNumbersFromSource(psiFile)
                val actualLineNumbers = extractActualLineNumbersFromBytecode(classFileFactory, true)
                assertFalse("Missed 'lineNumbers' calls in test data", expectedLineNumbers.isEmpty())
                KtUsefulTestCase.assertSameElements(actualLineNumbers, expectedLineNumbers)
            }
        } catch (e: Throwable) {
            println(classFileFactory.createText())
            throw rethrow(e)
        }
    }

    companion object {
        private const val LINE_NUMBER_FUN = "lineNumber"
        private val TEST_LINE_NUMBER_PATTERN = Pattern.compile("^.*test.$LINE_NUMBER_FUN\\(\\).*$")

        private fun createLineNumberDeclaration(): CodegenTestCase.TestFile {
            return CodegenTestCase.TestFile(
                "$LINE_NUMBER_FUN.kt",
                "package test;\n\npublic fun $LINE_NUMBER_FUN(): Int = 0\n"
            )
        }

        private fun getActualLineNumbersAsString(lines: List<String>): String {
            return lines.joinToString(" ", "// ", "", -1, "...") { lineNumber -> lineNumber }
        }

        private fun extractActualLineNumbersFromBytecode(factory: ClassFileFactory, testFunInvoke: Boolean): List<String> {
            val actualLineNumbers = Lists.newArrayList<String>()
            for (outputFile in factory.getClassFiles()) {
                val cr = ClassReader(outputFile.asByteArray())
                try {
                    val lineNumbers = if (testFunInvoke) readTestFunLineNumbers(cr) else readAllLineNumbers(cr)
                    actualLineNumbers.addAll(lineNumbers)
                } catch (e: Throwable) {
                    println(factory.createText())
                    throw rethrow(e)
                }

            }

            return actualLineNumbers
        }

        private fun extractSelectedLineNumbersFromSource(file: KtFile): List<String> {
            val fileContent = file.text
            val lineNumbers = Lists.newArrayList<String>()
            val lines = StringUtil.convertLineSeparators(fileContent).split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in lines.indices) {
                val matcher = TEST_LINE_NUMBER_PATTERN.matcher(lines[i])
                if (matcher.matches()) {
                    lineNumbers.add(Integer.toString(i + 1))
                }
            }

            return lineNumbers
        }

        private fun readTestFunLineNumbers(cr: ClassReader): List<String> {
            val labels = Lists.newArrayList<Label>()
            val labels2LineNumbers = HashMap<Label, String>()

            val visitor = object : ClassVisitor(Opcodes.ASM5) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    desc: String,
                    signature: String,
                    exceptions: Array<String>
                ): MethodVisitor {
                    return object : MethodVisitor(Opcodes.ASM5) {
                        private var lastLabel: Label? = null

                        override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                            if (LINE_NUMBER_FUN == name) {
                                assert(lastLabel != null) { "A function call with no preceding label" }
                                labels.add(lastLabel)
                            }
                            lastLabel = null
                        }

                        override fun visitLabel(label: Label) {
                            lastLabel = label
                        }

                        override fun visitLineNumber(line: Int, start: Label) {
                            labels2LineNumbers[start] = Integer.toString(line)
                        }
                    }
                }
            }

            cr.accept(visitor, ClassReader.SKIP_FRAMES)

            val lineNumbers = Lists.newArrayList<String>()
            for (label in labels) {
                val lineNumber = labels2LineNumbers[label] ?: error("No line number found for a label")
                lineNumbers.add(lineNumber)
            }

            return lineNumbers
        }

        private fun readAllLineNumbers(reader: ClassReader): List<String> {
            val result = ArrayList<String>()
            val visitedLabels = HashSet<String>()

            reader.accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    desc: String,
                    signature: String,
                    exceptions: Array<String>
                ): MethodVisitor {
                    return object : MethodVisitor(Opcodes.ASM5) {
                        override fun visitLineNumber(line: Int, label: Label) {
                            val overrides = !visitedLabels.add(label.toString())

                            result.add((if (overrides) "+" else "") + line)
                        }
                    }
                }
            }, ClassReader.SKIP_FRAMES)
            return result
        }
    }
}
