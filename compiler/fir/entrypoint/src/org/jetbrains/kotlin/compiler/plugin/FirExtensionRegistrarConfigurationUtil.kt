/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/*
 * The purpose of this function is to write
 *
 *   val extensions = configuration.getCompilerExtension(FirExtensionRegistrar)
 *
 * instead of
 *
 *   @Suppress("UNCHECKED_CAST")
 *   val extensions = configuration.getCompilerExtension(FirExtensionRegistrarAdapter) as List<FirExtensionRegistrar>
 *
 * An unused ` descriptor ` parameter is needed to keep the shape of the function the same as
 * in the overload with generic extension point descriptor. So when the `FirExtensionRegistrarAdapter` will be removed,
 * all call sites will call the generic-one `getCompilerExtension` function without any modification.
 */
@Suppress("unused")
fun CompilerConfiguration.getCompilerExtension(descriptor: FirExtensionRegistrar.Companion): List<FirExtensionRegistrar> {
    val extensions = getCompilerExtension(FirExtensionRegistrarAdapter)
    @Suppress("UNCHECKED_CAST")
    return extensions as List<FirExtensionRegistrar>
}

@OptIn(ExperimentalCompilerApi::class)
context(storage: CompilerPluginRegistrar.ExtensionStorage)
fun FirExtensionRegistrar.Companion.registerExtension(extension: FirExtensionRegistrar) {
    with(storage) {
        FirExtensionRegistrarAdapter.Companion.registerExtension(extension)
    }
}
