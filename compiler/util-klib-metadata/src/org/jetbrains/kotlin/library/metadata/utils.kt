/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.irProviderName

fun BaseKotlinLibrary.isInteropLibrary() = irProviderName == KLIB_INTEROP_IR_PROVIDER_IDENTIFIER

fun ModuleDescriptor.isFromInteropLibrary() =
    when (this) {
        is ModuleDescriptorImpl ->
            if (klibModuleOrigin !is DeserializedKlibModuleOrigin) false
            else kotlinLibrary.isInteropLibrary()
        else -> false // cinterop libraries are deserialized by Fir2Ir as ModuleDescriptorImpl, not FirModuleDescriptor
    }