/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

class CompiledClassesManager(val testServices: TestServices) : TestService {
    private val outputDirCache = mutableMapOf<TestModule, File>()

    fun getOutputDirForModule(module: TestModule): File {
        return outputDirCache.getOrPut(module) {
            testServices.getOrCreateTempDirectory("module_${module.name}_classes")
        }
    }
}

val TestServices.compiledClassesManager: CompiledClassesManager by TestServices.testServiceAccessor()
