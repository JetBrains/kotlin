/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KlibMetadataVersion(major: Int, minor: Int, patch: Int) : BinaryVersion(major, minor, patch) {

    override fun isCompatible(): Boolean = isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = KlibMetadataVersion(1, 0, 0)

        @JvmField
        val INVALID_VERSION = KlibMetadataVersion(-1, -1, -1)
    }
}

fun KlibMetadataVersion(vararg values: Int): KlibMetadataVersion {
    if (values.size != 3) error("Metadata version should be in major.minor.patch format: $values")
    return KlibMetadataVersion(values[0], values[1], values[2])
}

val KotlinLibrary.metadataVersion: KlibMetadataVersion
    get() {
        val versionString = manifestProperties.getProperty(KLIB_PROPERTY_METADATA_VERSION)
        val versionIntArray = BinaryVersion.parseVersionArray(versionString)
            ?: error("Could not parse metadata version: $versionString")
        return KlibMetadataVersion(*versionIntArray)
    }