/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.config

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

interface ContentRoot

data class KotlinSourceRoot(val path: String): ContentRoot

fun CompilerConfiguration.addKotlinSourceRoot(source: String) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, KotlinSourceRoot(source))
}

fun CompilerConfiguration.addKotlinSourceRoots(sources: List<String>): Unit =
    sources.forEach(this::addKotlinSourceRoot)

val CompilerConfiguration.kotlinSourceRoots: List<String>
    get() = get(CLIConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<KotlinSourceRoot>()?.map { it.path }.orEmpty()
