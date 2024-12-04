/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

abstract class RuntimeClasspathProvider(val testServices: TestServices) {
    abstract fun runtimeClassPaths(module: TestModule): List<File>
}

abstract class RuntimeClasspathJsProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return listOf(
            File(System.getProperty("kotlin.js.dom.api.compat")!!)
        )
    }
}

class RuntimeClasspathProvidersContainer(val providers: List<RuntimeClasspathProvider>) : TestService

private val TestServices.runtimeClasspathProviderContainer: RuntimeClasspathProvidersContainer by TestServices.testServiceAccessor()
val TestServices.runtimeClasspathProviders: List<RuntimeClasspathProvider>
    get() = runtimeClasspathProviderContainer.providers
