/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

abstract class BaseTest {
    companion object {
        val compilationService: CompilationService by lazy {
            CompilationService.loadImplementation(BaseTest::class.java.classLoader)
        }
        private val _compilerVersion by lazy {
            try {
                compilationService.getCompilerVersion()
            } catch (_: NoSuchMethodError) {
                "1.9.20" // getCompilerVersion is introduced since 2.0.0
            }
        }
        val compilerVersion: KotlinToolingVersion by lazy {
            KotlinToolingVersion(_compilerVersion)
        }
    }
}