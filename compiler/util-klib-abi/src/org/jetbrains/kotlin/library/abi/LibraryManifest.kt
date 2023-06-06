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
 * @property nativeTargets [KLIB_PROPERTY_NATIVE_TARGETS]
 * @property compilerVersion [KLIB_PROPERTY_COMPILER_VERSION]
 * @property abiVersion [KLIB_PROPERTY_ABI_VERSION]
 * @property libraryVersion [KLIB_PROPERTY_LIBRARY_VERSION]
 * @property irProviderName [KLIB_PROPERTY_IR_PROVIDER]
 */
@ExperimentalLibraryAbiReader
data class LibraryManifest(
    val platform: String?,
    val nativeTargets: List<String>,
    val compilerVersion: String?,
    val abiVersion: String?,
    val libraryVersion: String?,
    val irProviderName: String?
)
