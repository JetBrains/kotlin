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

/*
 * NB: ALL NECESSARY FLAGS ARE PASSED THROUGH Gradle
 */

@Suite
@SelectClasses(
    FirLightTreeBlackBoxCodegenTestGenerated::class,
    FirLightTreeBlackBoxInlineCodegenTestGenerated::class,

    FirPsiBlackBoxCodegenTestGenerated::class,
    FirPsiBlackBoxInlineCodegenTestGenerated::class,

    CoreJrtFsTest::class
)
@IncludeClassNamePatterns(".*Test.*Generated")
@ExcludeTags("<modernJava>")
annotation class CustomJvmTargetOnJvmBaseTest

// JDK 17
@CustomJvmTargetOnJvmBaseTest
class JvmTarget8OnJvm17

@CustomJvmTargetOnJvmBaseTest
class JvmTarget17OnJvm17


// LAST JDK from JdkMajorVersion available on machine
@CustomJvmTargetOnJvmBaseTest
class JvmTarget8OnJvmLast

@CustomJvmTargetOnJvmBaseTest
class JvmTargetLastOnJvmLast
