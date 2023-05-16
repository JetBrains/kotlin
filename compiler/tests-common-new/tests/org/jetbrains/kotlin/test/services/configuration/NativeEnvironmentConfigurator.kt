/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.runtimeClasspathProviders
import java.io.File

class NativeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private val nativeHome
            get() = System.getProperty("kotlin.internal.native.test.nativeHome")
                ?: error("No nativeHome provided. Are you sure the test are executed within :native:native.tests?")

        fun getRuntimePathsForModule(module: TestModule, testServices: TestServices): List<String> {
            val result = mutableListOf<String>()

            if (ConfigurationDirectives.WITH_STDLIB in module.directives) {
                result += File("$nativeHome/klib/common/stdlib").absolutePath
            }

            testServices.runtimeClasspathProviders
                .flatMap { it.runtimeClassPaths(module) }
                .mapTo(result) { it.absolutePath }

            return result
        }
    }
}