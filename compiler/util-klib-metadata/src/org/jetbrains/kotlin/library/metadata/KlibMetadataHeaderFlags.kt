/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_FLAGS
import org.jetbrains.kotlin.library.writer.KlibManifestWriterSpec
import org.jetbrains.kotlin.library.writer.KlibWriter
import java.util.Properties

/**
 * Metadata flags are stored in two places (see KT-87443):
 *  - since 2.5.0, in the manifest as a bitmask ([computeKlibMetadataFlagsMask]) under the [KLIB_PROPERTY_METADATA_FLAGS] property;
 *  - in the metadata header `flags` bitmask ([KlibMetadataProtoBuf.Header.flags]) - this is legacy and is to be dropped together with the
 *    header file in KT-87197.
 */
object KlibMetadataHeaderFlags {
    // Note: previously the value of this flag was 0x1.
    const val PRE_RELEASE = 0x2
}

fun addMetadataFlagsToManifest(manifestProperties: Properties, languageVersionSettings: LanguageVersionSettings) {
    val mask = computeKlibMetadataFlagsMask(languageVersionSettings)
    manifestProperties.setProperty(KLIB_PROPERTY_METADATA_FLAGS, mask.toString())
}

/**
 * A [KlibWriter] DSL extension to include metadata flags to the manifest file.
 */
fun KlibManifestWriterSpec.metadataFlags(languageVersionSettings: LanguageVersionSettings) {
    customProperties {
        addMetadataFlagsToManifest(this, languageVersionSettings)
    }
}

fun addMetadataFlagsToHeader(header: KlibMetadataProtoBuf.Header.Builder, languageVersionSettings: LanguageVersionSettings) {
    val mask = computeKlibMetadataFlagsMask(languageVersionSettings)
    if (mask != 0) {
        header.flags = mask
    }
}

fun computeKlibMetadataFlagsMask(languageVersionSettings: LanguageVersionSettings): Int {
    var mask = 0
    if (languageVersionSettings.isPreRelease()) mask = mask or KlibMetadataHeaderFlags.PRE_RELEASE
    return mask
}
