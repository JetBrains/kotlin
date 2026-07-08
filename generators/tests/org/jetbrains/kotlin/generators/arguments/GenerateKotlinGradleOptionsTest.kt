/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.generators.arguments.test

import org.jetbrains.kotlin.generators.arguments.generateGradleCompilerTypes
import org.jetbrains.kotlin.generators.arguments.generateKotlinGradleOptions
import org.jetbrains.kotlin.generators.arguments.getPrinterForTests
import org.junit.jupiter.api.Test

class GenerateKotlinGradleOptionsTest {
    @Test
    fun testKotlinGradleOptionsAreUpToDate() {
        generateKotlinGradleOptions(::getPrinterForTests)
    }

    @Test
    fun testKotlinGradleTypesAreUpToDate() {
        generateGradleCompilerTypes(::getPrinterForTests)
    }
}
