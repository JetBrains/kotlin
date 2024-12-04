/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.config

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

interface ContentRoot

/**
 * @param isCommon whether this source root contains sources of a common module in a multi-platform project
 */
data class KotlinSourceRoot(val path: String, val isCommon: Boolean, val hmppModuleName: String?): ContentRoot

@JvmOverloads
fun CompilerConfiguration.addKotlinSourceRoot(path: String, isCommon: Boolean = false, hmppModuleName: String? = null) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, KotlinSourceRoot(path, isCommon, hmppModuleName))
}

fun CompilerConfiguration.addKotlinSourceRoots(sources: List<String>): Unit =
    sources.forEach { addKotlinSourceRoot(it) }

val CompilerConfiguration.kotlinSourceRoots: List<KotlinSourceRoot>
    get() = get(CLIConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<KotlinSourceRoot>().orEmpty()
