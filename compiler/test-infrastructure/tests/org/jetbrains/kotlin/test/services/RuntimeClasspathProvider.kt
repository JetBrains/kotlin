/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import java.io.File

abstract class RuntimeClasspathProvider {
    abstract fun runtimeClassPaths(): List<File>
}

class RuntimeClasspathProviderHolder(val providers: List<RuntimeClasspathProvider>) : TestService

private val TestServices.runtimeClasspathProviderHolder: RuntimeClasspathProviderHolder by TestServices.testServiceAccessor()
val TestServices.runtimeClasspathProviders: List<RuntimeClasspathProvider>
    get() = runtimeClasspathProviderHolder.providers
