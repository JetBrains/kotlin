/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This service is used to provide main class and arguments for invoke of `box` method
 *   in separate JVM instance if it somehow differs from default implementation, which is
 *
 * fun main() {
 *     val result = box()
 *     if (result != "OK") {
 *         throw AssertionError()
 *     }
 * }
 *
 * Please note that can work only if running in separate JDK is enabled,
 *   see [CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS] directive
 *
 */
abstract class JvmBoxMainClassProvider(val testServices: TestServices) : TestService {
    abstract fun getMainClassNameAndAdditionalArguments(module: TestModule): List<String>
}

val TestServices.jvmBoxMainClassProvider: JvmBoxMainClassProvider? by TestServices.nullableTestServiceAccessor()
