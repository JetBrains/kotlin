/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.jdk

import org.jetbrains.kotlin.test.jvm.compiler.CoreJrtFsTest
import org.jetbrains.kotlin.test.runners.codegen.*
import org.junit.platform.suite.api.ExcludeTags
import org.junit.platform.suite.api.IncludeClassNamePatterns
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import runners.codegen.IrBlackBoxCodegenTestGenerated
import runners.codegen.IrBlackBoxInlineCodegenWithBytecodeInlinerTestGenerated
import runners.codegen.IrCompileKotlinAgainstInlineKotlinTestGenerated

/*
 * NB: ALL NECESSARY FLAGS ARE PASSED THROUGH Gradle
 */

@Suite
@SelectClasses(
    IrBlackBoxCodegenTestGenerated::class,
    IrBlackBoxInlineCodegenWithBytecodeInlinerTestGenerated::class,
    IrCompileKotlinAgainstInlineKotlinTestGenerated::class,

    FirLightTreeBlackBoxCodegenTestGenerated::class,
    FirLightTreeBlackBoxInlineCodegenWithBytecodeInlinerTestGenerated::class,
    FirLightTreeSerializeCompileKotlinAgainstInlineKotlinTestGenerated::class,

    FirPsiBlackBoxCodegenTestGenerated::class,
    FirPsiBlackBoxInlineCodegenWithBytecodeInlinerTestGenerated::class,
    FirPsiSerializeCompileKotlinAgainstInlineKotlinTestGenerated::class,

    CoreJrtFsTest::class
)
@IncludeClassNamePatterns(".*Test.*Generated")
@ExcludeTags("<modernJava>")
annotation class CustomJvmTargetOnJvmBaseTest

// JDK 8
@CustomJvmTargetOnJvmBaseTest
class JvmTarget8OnJvm8

// JDK 11
@CustomJvmTargetOnJvmBaseTest
class JvmTarget8OnJvm11

@CustomJvmTargetOnJvmBaseTest
class JvmTarget11OnJvm11

// JDK 15
@CustomJvmTargetOnJvmBaseTest
class JvmTarget8OnJvm15

@CustomJvmTargetOnJvmBaseTest
class JvmTarget15OnJvm15

@CustomJvmTargetOnJvmBaseTest
class JvmTarget8OnJvm17

@CustomJvmTargetOnJvmBaseTest
class JvmTarget17OnJvm17


// LAST JDK from JdkMajorVersion available on machine
@CustomJvmTargetOnJvmBaseTest
class JvmTarget8OnJvmLast

@CustomJvmTargetOnJvmBaseTest
class JvmTargetLastOnJvmLast
