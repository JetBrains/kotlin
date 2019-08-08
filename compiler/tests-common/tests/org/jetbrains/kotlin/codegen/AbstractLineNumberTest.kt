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

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

abstract class AbstractLineNumberTest : CodegenTestCase() {

    override fun doMultiFileTest(wholeFile: File, files: MutableList<TestFile>) {
        val isCustomTest = wholeFile.parentFile.name.equals("custom", ignoreCase = true)
        if (!isCustomTest) {
            files.add(createLineNumberDeclaration())
        }
        compile(files)

        val psiFile = myFiles.psiFiles.single { file -> file.name == wholeFile.name }

        try {
            if (isCustomTest) {
                compareCustom(psiFile, wholeFile)
            } else {
                val expectedLineNumbers = extractSelectedLineNumbersFromSource(psiFile)
                val actualLineNumbers = extractActualLineNumbersFromBytecode(classFileFactory, true)
                assertFalse("Missed 'lineNumbers' calls in test data", expectedLineNumbers.isEmpty())
                KtUsefulTestCase.assertSameElements(actualLineNumbers, expectedLineNumbers)
            }
        } catch (e: Throwable) {
            printReport(wholeFile)
            throw e
        }
    }

    protected open fun compareCustom(psiFile: KtFile, wholeFile: File) {
        val actualLineNumbers = extractActualLineNumbersFromBytecode(classFileFactory, false)
        val text = psiFile.text
        val newFileText = text.substring(0 until Regex("// \\d+").find(text)!!.range.first) +
                getActualLineNumbersAsString(actualLineNumbers)
        KotlinTestUtils.assertEqualsToFile(wholeFile, newFileText)
    }

    protected fun extractActualLineNumbersFromBytecode(factory: ClassFileFactory, testFunInvoke: Boolean) =
        factory.getClassFiles().flatMap { outputFile ->
            val cr = ClassReader(outputFile.asByteArray())
            if (testFunInvoke) readTestFunLineNumbers(cr) else readAllLineNumbers(cr)
        }

    protected open fun readTestFunLineNumbers(cr: ClassReader): List<String> {
        val labels = arrayListOf<Label>()
        val labels2LineNumbers = HashMap<Label, String>()

        val visitor = object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitMethod(
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<String>?
            ): MethodVisitor {
                return getTestFunLineNumbersMethodVisitor(labels, labels2LineNumbers)
            }
        }

        cr.accept(visitor, ClassReader.SKIP_FRAMES)

        return labels.map { label ->
            labels2LineNumbers[label] ?: error("No line number found for a label")
        }
    }

    protected open fun getTestFunLineNumbersMethodVisitor(
        labels: ArrayList<Label>,
        labels2LineNumbers: HashMap<Label, String>
    ): MethodVisitor {
        return object : MethodVisitor(Opcodes.API_VERSION) {
            private var lastLabel: Label? = null

            override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                if (LINE_NUMBER_FUN == name) {
                    labels.add(lastLabel ?: error("A function call with no preceding label"))
                }
                lastLabel = null
            }

            override fun visitLabel(label: Label) {
                lastLabel = label
            }

            override fun visitLineNumber(line: Int, start: Label) {
                labels2LineNumbers[start] = line.toString()
            }
        }
    }

    protected open fun readAllLineNumbers(reader: ClassReader): List<String> {
        val result = ArrayList<String>()
        val visitedLabels = HashSet<String>()

        reader.accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitMethod(
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<String>?
            ): MethodVisitor {
                return object : MethodVisitor(Opcodes.API_VERSION) {
                    override fun visitLineNumber(line: Int, label: Label) {
                        val overrides = !visitedLabels.add(label.toString())

                        result.add((if (overrides) "+" else "") + line)
                    }
                }
            }
        }, ClassReader.SKIP_FRAMES)
        return result
    }

    protected open fun extractSelectedLineNumbersFromSource(file: KtFile): List<String> {
        val fileContent = file.text
        val lineNumbers = arrayListOf<String>()
        val lines = StringUtil.convertLineSeparators(fileContent).split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (i in lines.indices) {
            val matcher = TEST_LINE_NUMBER_PATTERN.matcher(lines[i])
            if (matcher.matches()) {
                lineNumbers.add((i + 1).toString())
            }
        }

        return lineNumbers
    }

    companion object {
        const val LINE_NUMBER_FUN = "lineNumber"
        private val TEST_LINE_NUMBER_PATTERN = Pattern.compile("^.*test.$LINE_NUMBER_FUN\\(\\).*$")

        private fun createLineNumberDeclaration() =
            TestFile(
                "$LINE_NUMBER_FUN.kt",
                "package test;\n\npublic fun $LINE_NUMBER_FUN(): Int = 0\n"
            )

        private fun getActualLineNumbersAsString(lines: List<String>) =
            lines.joinToString(" ", "// ")
    }
}
