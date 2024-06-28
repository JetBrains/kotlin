/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.codegen.fir

import org.jetbrains.kotlin.codegen.ir.*
import org.jetbrains.kotlin.jvm.compiler.JvmIrLinkageModeTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.FirParser.LightTree

class FirLightTreePackageGenTest : IrPackageGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreePrimitiveTypesTest : IrPrimitiveTypesTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeAnnotationGenTest : IrAnnotationGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeOuterClassGenTest : IrOuterClassGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

abstract class AbstractFirLightTreeCheckLocalVariablesTableTest : AbstractIrCheckLocalVariablesTableTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreePropertyGenTest : IrPropertyGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeKotlinSyntheticClassAnnotationTest : IrKotlinSyntheticClassAnnotationTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeVarArgTest : IrVarArgTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeControlStructuresTest : IrControlStructuresTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeInnerClassInfoGenTest : IrInnerClassInfoGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeMethodOrderTest : IrMethodOrderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree

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
            listOf("<init>()V", "f3()V", "f0()V", "f4()V", "f2()V", "f1()V", "f5()V")
        )
    }
}

class FirLightTreeReflectionClassLoaderTest : IrReflectionClassLoaderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeCustomBytecodeTextTest : IrCustomBytecodeTextTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeCustomScriptCodegenTest : IrCustomScriptCodegenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeGenerateNotNullAssertionsTest : IrGenerateNotNullAssertionsTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeSourceInfoGenTest : IrSourceInfoGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeLinkageModeTest : JvmIrLinkageModeTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}
