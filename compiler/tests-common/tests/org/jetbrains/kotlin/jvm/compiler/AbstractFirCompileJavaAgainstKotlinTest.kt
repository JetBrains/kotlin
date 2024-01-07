/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirCompileJavaAgainstKotlinTest(private val useLightTree: Boolean) : AbstractCompileJavaAgainstKotlinTest() {
    override fun doTest(ktFilePath: String, useJavac: Boolean) {
        val ktFile = File(ktFilePath)
        val directives = KotlinTestUtils.parseDirectives(ktFile.readText())
        if (directives.contains("IGNORE_FIR")) return
        super.doTest(ktFilePath, useJavac)
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(CommonConfigurationKeys.USE_FIR, true)
        if (useLightTree) configuration.put(CommonConfigurationKeys.USE_LIGHT_TREE, true)
    }
}
