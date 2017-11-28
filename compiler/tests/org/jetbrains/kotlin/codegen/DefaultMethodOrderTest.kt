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

package org.jetbrains.kotlin.codegen

import junit.framework.TestCase
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

/**
 * Copied from MethodOrderTest
 *
 *
 */
class DefaultMethodOrderTest : CodegenTestCase() {
    fun testDeterministicDefaultMethodImplOrder() {
        doTest(
                """
                    interface Base<K, V> {
                        fun getSize(): Int = 5
                        fun size(): Int = getSize()
                        fun getKeys(): Int = 4
                        fun keySet() = getKeys()
                        fun getEntries(): Int = 3
                        fun entrySet() = getEntries()
                        fun getValues(): Int = 2
                        fun values() = getValues()

                        fun removeEldestEntry(eldest: Any?): Boolean
                    }

                    class MinMap<K, V> : Base<K, V> {
                        override fun removeEldestEntry(eldest: Any?) = true
                    }
                """,
                "MinMap",
                listOf("removeEldestEntry(Ljava/lang/Object;)Z", "<init>()V", "getSize()I", "size()I", "getKeys()I", "keySet()I", "getEntries()I", "entrySet()I", "getValues()I", "values()I")
        )
    }

    private fun doTest(sourceText: String, classSuffix: String, expectedOrder: List<String>) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY)
        myFiles = CodegenTestFiles.create("file.kt", sourceText, myEnvironment!!.project)

        val classFileForObject = generateClassesInFile().asList().first { it.relativePath.endsWith("$classSuffix.class") }
        val classReader = ClassReader(classFileForObject.asByteArray())

        val methodNames = ArrayList<String>()

        classReader.accept(object : ClassVisitor(Opcodes.ASM4) {
            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                methodNames.add(name + desc)
                return null
            }
        }, ClassReader.SKIP_CODE and ClassReader.SKIP_DEBUG and ClassReader.SKIP_FRAMES)

        TestCase.assertEquals(expectedOrder, methodNames)
    }
}
