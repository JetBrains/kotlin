/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.codegen.fir

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.ir.AbstractIrCheckLocalVariablesTableTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.FirParser.LightTree

class FirLightTreePackageGenTest : PackageGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreePrimitiveTypesTest : PrimitiveTypesTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeAnnotationGenTest : AnnotationGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeOuterClassGenTest : OuterClassGenTest() {
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

class FirLightTreePropertyGenTest : PropertyGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeKotlinSyntheticClassAnnotationTest : KotlinSyntheticClassAnnotationTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeVarArgTest : VarArgTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeControlStructuresTest : ControlStructuresTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeInnerClassInfoGenTest : InnerClassInfoGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeMethodOrderTest : MethodOrderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree

    override fun delegatedMethodExpectation(): List<String> =
        listOf("<init>()V", "f3()V", "f0()V", "f4()V", "f2()V", "f1()V", "f5()V")
}

class FirLightTreeReflectionClassLoaderTest : ReflectionClassLoaderTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeCustomBytecodeTextTest : CustomBytecodeTextTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeGenerateNotNullAssertionsTest : GenerateNotNullAssertionsTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}

class FirLightTreeSourceInfoGenTest : SourceInfoGenTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = LightTree
}
