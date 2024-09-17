/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.codegen.fir

import org.jetbrains.kotlin.codegen.CustomBytecodeTextTest
import org.jetbrains.kotlin.codegen.MethodOrderTest
import org.jetbrains.kotlin.codegen.ir.*
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

class FirPsiMethodOrderTest : MethodOrderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi

    override fun delegatedMethodExpectation(): List<String> =
        listOf("<init>()V", "f3()V", "f0()V", "f4()V", "f2()V", "f1()V", "f5()V")
}

class FirPsiReflectionClassLoaderTest : IrReflectionClassLoaderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = Psi
}

class FirPsiCustomBytecodeTextTest : CustomBytecodeTextTest() {
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
