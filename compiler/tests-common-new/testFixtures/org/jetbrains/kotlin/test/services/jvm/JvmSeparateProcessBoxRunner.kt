/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File
import java.net.URL

/**
 * Runs `box` of tests that need a separate JVM (see [org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS])
 * in JVMs managed by the service, e.g., JVMs reused by many tests, instead of the new JVM that [JvmBoxRunner] starts for each test.
 */
abstract class JvmSeparateProcessBoxRunner(val testServices: TestServices) : TestService {
    /**
     * Runs `box` of [module] on the JVM of [javaExe] with [classPath]. Returns `OK` if `box` succeeded and a description of the failure
     * otherwise, or `null` to let [JvmBoxRunner] start a new JVM for the test.
     */
    abstract fun runBox(module: TestModule, javaExe: File, classPath: List<URL>): String?
}

val TestServices.jvmSeparateProcessBoxRunner: JvmSeparateProcessBoxRunner? by TestServices.nullableTestServiceAccessor()
