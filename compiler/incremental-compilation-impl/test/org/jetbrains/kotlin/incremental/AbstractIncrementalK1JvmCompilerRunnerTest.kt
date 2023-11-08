/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.LanguageVersion
import java.io.File

abstract class AbstractIncrementalK1JvmCompilerRunnerTest : AbstractIncrementalJvmCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            if (LanguageVersion.LATEST_STABLE >= LanguageVersion.KOTLIN_2_0) {
                languageVersion = "1.9"
            }
        }

}