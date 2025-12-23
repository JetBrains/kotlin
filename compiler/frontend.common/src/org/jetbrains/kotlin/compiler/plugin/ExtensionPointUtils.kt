/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugin

import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor

@OptIn(ExperimentalCompilerApi::class)
fun <T : Any> CompilerConfiguration.getCompilerExtension(descriptor: ExtensionPointDescriptor<T>): List<T> {
    val extensionStorage = this.extensionsStorage ?: error("Extensions storage is not registered")
    return extensionStorage[descriptor]
}
