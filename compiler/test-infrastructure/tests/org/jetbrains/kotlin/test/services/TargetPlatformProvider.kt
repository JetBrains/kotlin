/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.TestModule

/**
 * The [TargetPlatform] of a module has different meaning in different kinds of tests:
 * - in compiler tests, all modules in the test have the same target platform (reflecting the behavior of a regular compilation)
 * - in Analysis API tests each module might have its own target platform (reflecting the MPP analysis of IDE)
 *
 * To properly handle this difference, this service is used.
 * It allows configuring how the target platform is determined for a module based on default settings, directives and module name
 */
abstract class TargetPlatformProvider : TestService {
    abstract fun getTargetPlatform(module: TestModule): TargetPlatform
}

val TestServices.targetPlatformProvider: TargetPlatformProvider by TestServices.testServiceAccessor()

fun TestModule.targetPlatform(testServices: TestServices): TargetPlatform =
    testServices.targetPlatformProvider.getTargetPlatform(this)

class TargetPlatformProviderForCompilerTests(val testServices: TestServices) : TargetPlatformProvider() {
    override fun getTargetPlatform(module: TestModule): TargetPlatform {
        @OptIn(TestInfrastructureInternals::class)
        return testServices.defaultsProvider.targetPlatform
    }
}
