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
import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * Test correctness of written local variables in class file for specified method
 */
abstract class AbstractCheckLocalVariablesTableTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        compile(files)

        try {
            val classAndMethod = parseClassAndMethodSignature(wholeFile)
            val split = classAndMethod.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            assert(split.size == 2) { "Exactly one dot is expected: $classAndMethod" }
            val classFileRegex = StringUtil.escapeToRegexp(split[0] + ".class").replace("\\*", ".+")
            val methodName = split[1]

            val outputFiles = (classFileFactory as OutputFileCollection).asList()
            val outputFile = outputFiles.first { file -> file.relativePath.matches(classFileRegex.toRegex()) }

            val pathsString = outputFiles.joinToString { it.relativePath }
            assertNotNull("Couldn't find class file for pattern $classFileRegex in: $pathsString", outputFile)

            val actualLocalVariables = readLocalVariable(ClassReader(outputFile.asByteArray()), methodName)

            doCompare(wholeFile, files.single().content, actualLocalVariables)
        } catch (e: Throwable) {
            printReport(wholeFile)
            throw e
        }
    }

    protected open fun doCompare(
        testFile: File,
        text: String,
        actualLocalVariables: List<LocalVariable>
    ) {
        KotlinTestUtils.assertEqualsToFile(
            testFile,
            text.substringBefore("// VARIABLE : ") + getActualVariablesAsString(
                actualLocalVariables
            )
        )
    }

    protected class LocalVariable internal constructor(
        private val name: String,
        private val type: String,
        private val index: Int
    ) {

        override fun toString(): String {
            return "// VARIABLE : NAME=$name TYPE=$type INDEX=$index"
        }
    }

    private fun parseClassAndMethodSignature(testFile: File): String {
        for (line in testFile.readLines()) {
            val methodMatcher = methodPattern.matcher(line)
            if (methodMatcher.matches()) {
                return methodMatcher.group(1)
            }
        }

        throw AssertionError("method instructions not found")
    }

    companion object {

        private fun getActualVariablesAsString(list: List<LocalVariable>) = list.joinToString("\n")

        private val methodPattern = Pattern.compile("^// METHOD : *(.*)")

        private fun readLocalVariable(cr: ClassReader, methodName: String): List<LocalVariable> {

            class Visitor : ClassVisitor(Opcodes.API_VERSION) {
                var readVariables: MutableList<LocalVariable> = ArrayList()
                var methodFound = false

                override fun visitMethod(
                    access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?
                ): MethodVisitor? {
                    return if (methodName == name + desc) {
                        methodFound = true
                        object : MethodVisitor(Opcodes.API_VERSION) {
                            override fun visitLocalVariable(
                                name: String, desc: String, signature: String?, start: Label, end: Label, index: Int
                            ) {
                                readVariables.add(LocalVariable(name, desc, index))
                            }
                        }
                    } else {
                        super.visitMethod(access, name, desc, signature, exceptions)
                    }
                }
            }

            val visitor = Visitor()

            cr.accept(visitor, ClassReader.SKIP_FRAMES)

            TestCase.assertTrue("method not found: $methodName", visitor.methodFound)

            return visitor.readVariables
        }
    }
}

