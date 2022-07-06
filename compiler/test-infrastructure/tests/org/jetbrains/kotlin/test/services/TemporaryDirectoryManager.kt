/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import java.io.File

abstract class TemporaryDirectoryManager(protected val testServices: TestServices) : TestService {
    abstract fun getOrCreateTempDirectory(name: String): File
    abstract fun cleanupTemporaryDirectories()
    abstract val rootDir: File
}

val TestServices.temporaryDirectoryManager: TemporaryDirectoryManager by TestServices.testServiceAccessor()

fun TestServices.getOrCreateTempDirectory(name: String): File {
    return temporaryDirectoryManager.getOrCreateTempDirectory(name)
}
