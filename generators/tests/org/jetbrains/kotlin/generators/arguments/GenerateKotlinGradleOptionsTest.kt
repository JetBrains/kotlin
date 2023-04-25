/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.generators.arguments.test

import junit.framework.TestCase
import org.jetbrains.kotlin.generators.arguments.generateGradleCompilerTypes
import org.jetbrains.kotlin.generators.arguments.generateKotlinGradleOptions
import org.jetbrains.kotlin.generators.arguments.getPrinterForTests

class GenerateKotlinGradleOptionsTest : TestCase() {
    fun testKotlinGradleOptionsAreUpToDate() {
        generateKotlinGradleOptions(::getPrinterForTests)
    }

    fun testKotlinGradleTypesAreUpToDate() {
        generateGradleCompilerTypes(::getPrinterForTests)
    }
}
