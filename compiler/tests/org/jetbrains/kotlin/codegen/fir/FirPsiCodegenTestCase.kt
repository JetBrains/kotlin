/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.codegen.fir

import org.jetbrains.kotlin.codegen.ir.*
import org.jetbrains.kotlin.jvm.compiler.JvmIrLinkageModeTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.FirParser.Psi

class FirPsiPackageGenTest : IrPackageGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiPrimitiveTypesTest : IrPrimitiveTypesTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiAnnotationGenTest : IrAnnotationGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiOuterClassGenTest : IrOuterClassGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

abstract class AbstractFirPsiCheckLocalVariablesTableTest : AbstractIrCheckLocalVariablesTableTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiPropertyGenTest : IrPropertyGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiKotlinSyntheticClassAnnotationTest : IrKotlinSyntheticClassAnnotationTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiVarArgTest : IrVarArgTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiControlStructuresTest : IrControlStructuresTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiInnerClassInfoGenTest : IrInnerClassInfoGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiMethodOrderTest : IrMethodOrderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi

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

class FirPsiReflectionClassLoaderTest : IrReflectionClassLoaderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiCustomBytecodeTextTest : IrCustomBytecodeTextTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiCustomScriptCodegenTest : IrCustomScriptCodegenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiGenerateNotNullAssertionsTest : IrGenerateNotNullAssertionsTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiSourceInfoGenTest : IrSourceInfoGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiLinkageModeTest : JvmIrLinkageModeTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}
