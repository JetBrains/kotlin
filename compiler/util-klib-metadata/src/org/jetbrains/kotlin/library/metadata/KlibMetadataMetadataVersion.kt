/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KlibMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {

    override fun isCompatible(): Boolean = isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = KlibMetadataVersion(1, 4, 1)

        @JvmField
        val INVALID_VERSION = KlibMetadataVersion()
    }
}

val KotlinLibrary.metadataVersion: KlibMetadataVersion?
    get() {
        val versionString = manifestProperties.getProperty(KLIB_PROPERTY_METADATA_VERSION) ?: return null
        val versionIntArray = BinaryVersion.parseVersionArray(versionString) ?: return null
        return KlibMetadataVersion(*versionIntArray)
    }
