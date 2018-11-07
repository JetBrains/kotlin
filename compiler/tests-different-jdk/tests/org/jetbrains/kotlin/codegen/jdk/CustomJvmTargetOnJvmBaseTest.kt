/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.jdk

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.test.SuiteRunnerForCustomJdk
import org.jetbrains.kotlin.test.RunOnlyJdk6Test
import org.junit.runner.RunWith
import org.junit.runners.Suite

/*
 * NB: ALL NECESSARY FLAGS ARE PASSED THROUGH Gradle
 */

@Suite.SuiteClasses(
    BlackBoxCodegenTestGenerated::class,
    BlackBoxInlineCodegenTestGenerated::class,
    CompileKotlinAgainstInlineKotlinTestGenerated::class,
    CompileKotlinAgainstKotlinTestGenerated::class,
    BlackBoxAgainstJavaCodegenTestGenerated::class
)
abstract class CustomJvmTargetOnJvmBaseTest

@RunOnlyJdk6Test
@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget6OnJvm6 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget8OnJvm8 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget6OnJvm9 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget8OnJvm9 : CustomJvmTargetOnJvmBaseTest()

@RunWith(SuiteRunnerForCustomJdk::class)
class JvmTarget9OnJvm9 : CustomJvmTargetOnJvmBaseTest()