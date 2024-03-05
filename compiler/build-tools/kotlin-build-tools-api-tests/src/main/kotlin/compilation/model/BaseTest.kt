/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationService

abstract class BaseTest {
    companion object {
        val compilationService: CompilationService by lazy {
            CompilationService.loadImplementation(BaseTest::class.java.classLoader)
        }
    }
}