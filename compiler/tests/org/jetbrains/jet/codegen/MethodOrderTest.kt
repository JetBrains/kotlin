/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen

import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.asm4.ClassReader
import org.jetbrains.asm4.ClassVisitor
import org.jetbrains.asm4.Opcodes
import org.jetbrains.asm4.MethodVisitor
import java.util.ArrayList
import junit.framework.TestCase

public class MethodOrderTest: CodegenTestCase() {
    public fun testDelegatedMethod() {
        doTest(
                """
                    trait Trait {
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
                listOf("<clinit>()V", "f3()V", "<init>()V", "f0()V", "f1()V", "f2()V", "f4()V", "f5()V")
        )
    }

    public fun testLambdaClosureOrdering() {
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

    public fun testAnonymousObjectClosureOrdering() {
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
                listOf("<clinit>()V", "run()V", "<init>(LKlass;Ljava/lang/Object;Ljava/lang/String;IDLjava/lang/Object;J)V")
        )
    }

    public fun testMemberAccessor() {
        doTest(
                """
                    class Outer(private val a: Int, private var b: String) {
                        private fun c() {
                        }

                        inner class Inner() {
                            {
                                b = b + a
                                c()
                            }
                        }
                    }
                """,
                "Outer",
                listOf("<clinit>()V", "c()V", "<init>(ILjava/lang/String;)V", "getB\$b$0(LOuter;)Ljava/lang/String;", "setB\$b$0(LOuter;Ljava/lang/String;)V", "getA\$b$1(LOuter;)I", "c\$b$2(LOuter;)V")
        )
    }

    private fun doTest(sourceText: String, classSuffix: String, expectedOrder: List<String>) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS)
        myFiles = CodegenTestFiles.create("file.kt", sourceText, myEnvironment!!.getProject())

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