/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.commonizerTarget
import org.jetbrains.kotlin.library.interopFlag
import org.jetbrains.kotlin.library.irProviderName

/**
 * Genuine C-interop library always has two properties in manifest: `interop=true` and the `ir_provider` that
 * points to the known IR provider dedicated specifically for C-interop libraries.
 */
fun BaseKotlinLibrary.isCInteropLibrary(): Boolean =
    interopFlag == "true" && irProviderName == KLIB_INTEROP_IR_PROVIDER_IDENTIFIER

/**
 * Commonized C-interop library has two properties in manifest: `interop=true` and some non-empty `commonizer_target`.
 * The `ir_provider` is missing for commonized libraries, as no IR was ever supposed to be stored or anyhow provided
 * by such libraries.
 */
fun BaseKotlinLibrary.isCommonizedCInteropLibrary(): Boolean =
    interopFlag == "true" && commonizerTarget != null

@Deprecated(
    "Use BaseKotlinLibrary.isCInteropLibrary() for more precise check",
    ReplaceWith("isCInteropLibrary()", "org.jetbrains.kotlin.library.metadata.isCInteropLibrary"),
    DeprecationLevel.ERROR
)
fun BaseKotlinLibrary.isInteropLibrary() = irProviderName == KLIB_INTEROP_IR_PROVIDER_IDENTIFIER

@Deprecated(
    "Use isFromCInteropLibrary() instead",
    ReplaceWith("isFromCInteropLibrary()", "org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary"),
    DeprecationLevel.ERROR
)
fun ModuleDescriptor.isFromInteropLibrary(): Boolean {
    return if (this is ModuleDescriptorImpl) {
        // cinterop libraries are deserialized by Fir2Ir as ModuleDescriptorImpl, not FirModuleDescriptor
        klibModuleOrigin.isCInteropLibrary()
    } else false
}
