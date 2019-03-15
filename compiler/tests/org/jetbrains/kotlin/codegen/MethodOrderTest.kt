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

package org.jetbrains.kotlin.codegen

import junit.framework.TestCase
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class MethodOrderTest: CodegenTestCase() {
    fun testDelegatedMethod() {
        doTest(
                """
                    interface Trait {
                        fun f0()
                        fun f4()
                        fun f3()
                        fun f2()
                        fun f1()
                        fun f5()
                    }

                    val delegate: Trait = throw Error()

                    val obj = object : Trait by delegate {
                        override fun f3() { }
                    }
                """,
                "\$obj$1",
                listOf("f3()V", "<init>()V", "f0()V", "f1()V", "f2()V", "f4()V", "f5()V")
        )
    }

    fun testLambdaClosureOrdering() {
        doTest(
                """
                    class Klass {
                        fun Any.f(a: String, b: Int, c: Double, d: Any, e: Long) {
                           { a + b + c + d + e + this@f + this@Klass }()
                        }
                    }
                """,
                "\$f$1",
                listOf("invoke()Ljava/lang/Object;", "invoke()Ljava/lang/String;", "<init>(LKlass;Ljava/lang/Object;Ljava/lang/String;IDLjava/lang/Object;J)V")
        )
    }

    fun testAnonymousObjectClosureOrdering() {
        doTest(
                """
                    class Klass {
                        fun Any.f(a: String, b: Int, c: Double, d: Any, e: Long) {
                            object : Runnable {
                                override fun run() {
                                    a + b + c + d + e + this@f + this@Klass
                                }
                            }.run()
                        }
                    }
                """,
                "\$f$1",
                listOf("run()V", "<init>(LKlass;Ljava/lang/Object;Ljava/lang/String;IDLjava/lang/Object;J)V")
        )
    }

    fun testMemberAccessor() {
        doTest(
                """
                    class Outer(private val a: Int, private var b: String) {
                        private fun c() {
                        }

                        inner class Inner() {
                            init {
                                b = b + a
                                c()
                            }
                        }
                    }
                """,
                "Outer",
                listOf(
                        "c()V",
                        "<init>(ILjava/lang/String;)V",
                        "access\$getB\$p(LOuter;)Ljava/lang/String;",
                        "access\$setB\$p(LOuter;Ljava/lang/String;)V",
                        "access\$getA\$p(LOuter;)I",
                        "access\$c(LOuter;)V"
                )
        )
    }

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

    fun testBridgeOrder() {
        doTest(
            """
                interface IrElement
                class IrClassContext

                interface IrElementVisitor<out R, in D> {
                    fun visitElement(element: IrElement, data: D): R
                }

                interface IrElementTransformer<in D> : IrElementVisitor<IrElement, D> {
                    override fun visitElement(element: IrElement, data: D): IrElement =
                            element.also { throw RuntimeException() }
                }

                abstract class ClassLowerWithContext : IrElementTransformer<IrClassContext?>
            """,
            "ClassLowerWithContext",
            listOf(
                "<init>()V",
                "visitElement(LIrElement;LIrClassContext;)LIrElement;",
                "visitElement(LIrElement;Ljava/lang/Object;)Ljava/lang/Object;",
                "visitElement(LIrElement;Ljava/lang/Object;)LIrElement;"
            )
        )
    }

    private fun doTest(sourceText: String, classSuffix: String, expectedOrder: List<String>) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY)
        myFiles = CodegenTestFiles.create("file.kt", sourceText, myEnvironment!!.project)

        val classFileForObject = generateClassesInFile().asList().first { it.relativePath.endsWith("$classSuffix.class") }
        val classReader = ClassReader(classFileForObject.asByteArray())

        val methodNames = ArrayList<String>()

        classReader.accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                methodNames.add(name + desc)
                return null
            }
        }, ClassReader.SKIP_CODE and ClassReader.SKIP_DEBUG and ClassReader.SKIP_FRAMES)

        TestCase.assertEquals(expectedOrder, methodNames)
    }
}
