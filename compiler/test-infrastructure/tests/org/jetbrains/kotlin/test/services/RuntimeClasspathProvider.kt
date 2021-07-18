/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import java.io.File

interface RuntimeClasspathProvider : TestService {
    fun runtimeClassPaths():List<File>

    object Empty : RuntimeClasspathProvider {
        override fun runtimeClassPaths(): List<File> = emptyList()
    }
}

val TestServices.runtimeClasspathProvider: RuntimeClasspathProvider by TestServices.testServiceAccessor()