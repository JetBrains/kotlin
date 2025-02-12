/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.metadataVersion
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.util.*

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

private val LANGUAGE_TO_KLIB_METADATA_VERSION = EnumMap<LanguageVersion, MetadataVersion>(LanguageVersion::class.java).apply {
    LanguageVersion.entries.forEach { this[it] = KLIB_LEGACY_METADATA_VERSION }
    // TODO KT-74417 Uncomment in version 2.3 to bump metadata version
    this[LanguageVersion.KOTLIN_2_3] = KLIB_LEGACY_METADATA_VERSION // MetadataVersion(2, 3, 0)

    check(size == LanguageVersion.entries.size) {
        "Please add mappings from the missing LanguageVersion instances to the corresponding MetadataVersion " +
                "in `LANGUAGE_TO_KLIB_METADATA_VERSION`"
    }
}

private fun LanguageVersion.toMetadataVersion(): MetadataVersion = LANGUAGE_TO_KLIB_METADATA_VERSION.getValue(this)

fun CompilerConfiguration.metadataVersionOrDefault(
    languageVersion: LanguageVersion = languageVersionSettings.languageVersion
): MetadataVersion {
    return this.metadataVersion as? MetadataVersion ?: languageVersion.toMetadataVersion()
}
