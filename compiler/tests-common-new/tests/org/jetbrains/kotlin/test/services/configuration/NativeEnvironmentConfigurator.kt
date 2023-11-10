/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.konan.target.TargetSupportException
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

            if (ConfigurationDirectives.WITH_PLATFORM_LIBS in module.directives) {
                File("$nativeHome/klib/platform/${targetOsArch()}").listFiles()?.forEach {
                    result += it.absolutePath
                }
            }

            testServices.runtimeClasspathProviders
                .flatMap { it.runtimeClassPaths(module) }
                .mapTo(result) { it.absolutePath }

            return result
        }
    }
}

// `:native` module cannot be imported here, so property name and functions are just copy/pasted here
// `TARGET_PROPERTY` is same as org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty.TEST_TARGET.propertyName
private val TEST_TARGET_FULL_PROPERTY_NAME = "kotlin.internal.native.test.target"

private fun targetOsArch(): String {
    return System.getProperty(TEST_TARGET_FULL_PROPERTY_NAME) ?: "${hostOs()}_${hostArch()}"
}

// `hostOs()` and `hostArch()` are similar to functions in org.jetbrains.kotlin.konan.target.HostManager
private fun hostOs(): String {
    val javaOsName = System.getProperty("os.name")
    return when {
        javaOsName == "Mac OS X" -> "macos"
        javaOsName == "Linux" -> "linux"
        javaOsName.startsWith("Windows") -> "mingw"
        else -> throw TargetSupportException("Unknown operating system: $javaOsName")
    }
}

fun hostArch(): String =
    when (val arch = System.getProperty("os.arch")) {
        "x86_64" -> "x64"
        "amd64" -> "x64"
        "arm64" -> "arm64"
        "aarch64" -> "arm64"
        else -> throw TargetSupportException("Unknown hardware platform: $arch")
    }
