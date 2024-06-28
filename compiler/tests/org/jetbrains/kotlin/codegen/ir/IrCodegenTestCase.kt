/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.codegen.ir

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmClosureGenerationScheme
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TargetBackend.JVM_IR

open class IrPackageGenTest : PackageGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrPrimitiveTypesTest : PrimitiveTypesTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrAnnotationGenTest : AnnotationGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrOuterClassGenTest : OuterClassGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    // Class lambda not generated

    override fun testLambdaInConstructor() {
    }

    override fun testLambdaInInlineFunction() {
    }

    override fun testLambdaInInlineLambda() {
    }

    override fun testLambdaInLambdaInlinedIntoObject() {
    }

    override fun testLambdaInLambdaInlinedIntoObject2() {
    }

    override fun testLambdaInNoInlineFun() {
    }

    override fun testLambdaInlined() {
    }

    override fun testLocalObjectInInlineLambda() {
    }

    override fun testLocalObjectInLambdaInlinedIntoObject2() {
    }
}

open class IrPropertyGenTest : PropertyGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrKotlinSyntheticClassAnnotationTest : KotlinSyntheticClassAnnotationTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        // Current tests rely on class lambdas
        configuration.put(JVMConfigurationKeys.LAMBDAS, JvmClosureGenerationScheme.CLASS)
        configuration.put(JVMConfigurationKeys.SAM_CONVERSIONS, JvmClosureGenerationScheme.CLASS)
        super.updateConfiguration(configuration)
    }

    override fun testLocalFunction() {
        // Indy is generated, irrelevant test
    }
}

open class IrVarArgTest : VarArgTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrControlStructuresTest : ControlStructuresTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    override fun testCompareToNonnullableNotEq() {
        // https://youtrack.jetbrains.com/issue/KT-65357
    }
}

open class IrInnerClassInfoGenTest : InnerClassInfoGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    // Test is irrelevant with indy lambdas.

    override fun testLambdaClassFlags() {
    }
}

open class IrMethodOrderTest : MethodOrderTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    // Test is irrelevant with indy lambdas.
    override fun testLambdaClosureOrdering() {
    }

    override fun testDelegatedMethod() {
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
            listOf( "<init>()V", "f0()V", "f1()V", "f2()V", "f4()V", "f5()V", "f3()V")
        )
    }

    override fun testAnonymousObjectClosureOrdering() {
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
            listOf("<init>(Ljava/lang/String;IDLjava/lang/Object;JLjava/lang/Object;LKlass;)V", "run()V")
        )
    }

    override fun testBridgeOrder() {
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
                "visitElement(LIrElement;Ljava/lang/Object;)LIrElement;",
                "visitElement(LIrElement;Ljava/lang/Object;)Ljava/lang/Object;",
            )
        )
    }

    override fun testMemberAccessor() {
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
                "<init>(ILjava/lang/String;)V",
                "c()V",
                "access\$setB\$p(LOuter;Ljava/lang/String;)V",
                "access\$getB\$p(LOuter;)Ljava/lang/String;",
                "access\$getA\$p(LOuter;)I",
                "access\$c(LOuter;)V"
            )
        )
    }

    override fun testDeterministicDefaultMethodImplOrder() {
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
            listOf(
                "<init>()V",
                "removeEldestEntry(Ljava/lang/Object;)Z",
                "getSize()I",
                "size()I",
                "getKeys()I",
                "keySet()I",
                "getEntries()I",
                "entrySet()I",
                "getValues()I",
                "values()I"
            )
        )
    }
}

open class IrReflectionClassLoaderTest : ReflectionClassLoaderTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrCustomScriptCodegenTest : CustomScriptCodegenTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    override fun testAnnotatedDefinition() {
        // Discussing
    }
}

open class IrSourceInfoGenTest : SourceInfoGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}
