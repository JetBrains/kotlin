/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.*

/**
 * Anything that can be retrieved from manifest and that might be helpful to know about the inspected KLIB.
 *
 * @property platform [KLIB_PROPERTY_BUILTINS_PLATFORM]
 * @property platformTargets [KLIB_PROPERTY_NATIVE_TARGETS], [KLIB_PROPERTY_WASM_TARGETS]
 * @property compilerVersion [KLIB_PROPERTY_COMPILER_VERSION]
 * @property abiVersion [KLIB_PROPERTY_ABI_VERSION]
 * @property irProviderName [KLIB_PROPERTY_IR_PROVIDER]
 */
@ExperimentalLibraryAbiReader
data class LibraryManifest(
    val platform: String?,
    val platformTargets: List<LibraryTarget>,
    val compilerVersion: String?,
    val abiVersion: String?,
    val irProviderName: String?
) {
    @Deprecated("Use platformTargets instead", ReplaceWith("platformTargets"))
    val nativeTargets: List<String> get() = platformTargets.filterIsInstance<LibraryTarget.Native>().map { it.name }
}

/**
 * The concrete platform target that the library supports.
 */
sealed interface LibraryTarget {
    data class Native(val name: String) : LibraryTarget
    data class WASM(val name: String) : LibraryTarget
}
