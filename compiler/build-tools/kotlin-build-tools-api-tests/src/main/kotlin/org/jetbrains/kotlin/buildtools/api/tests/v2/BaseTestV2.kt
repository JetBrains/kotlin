/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests.v2

import org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain

abstract class BaseTestV2 {
    companion object {
        val kotlinToolchain: KotlinToolchain by lazy {
            KotlinToolchain.loadImplementation(BaseTestV2::class.java.classLoader)
        }

//        private val _compilerVersion by lazy {
//            try {
//                compilationService.getCompilerVersion()
//            } catch (_: NoSuchMethodError) {
//                "1.9.20" // getCompilerVersion is introduced since 2.0.0
//            }
//        }
//        val compilerVersion: KotlinToolingVersion by lazy {
//            KotlinToolingVersion(_compilerVersion)
//        }
    }
}