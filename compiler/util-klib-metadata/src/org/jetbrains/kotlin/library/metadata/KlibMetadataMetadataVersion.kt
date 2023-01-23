/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

/**
 * The version for `KlibMetadataProtoBuf.proto`. This version also includes the version of the core protobuf messages (`metadata.proto`).
 *
 * This version must be bumped when:
 * - Incompatible changes are made in `KlibMetadataProtoBuf.proto`
 * - Incompatible changes are made in `metadata.proto`
 * - Incompatible changes are made in Klib metadata serialization/deserialization logic
 *
 * The version bump must obey [org.jetbrains.kotlin.metadata.deserialization.BinaryVersion] rules (See `BinaryVersion` KDoc).
 *
 * Known bugs: [KlibMetadataVersion] isn't currently checked for klibs [KT-55808](https://youtrack.jetbrains.com/issue/KT-55808)
 * [KT-56062](https://youtrack.jetbrains.com/issue/KT-56062).
 */
class KlibMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {

    override fun isCompatibleWithCurrentCompilerVersion(): Boolean = isCompatibleTo(INSTANCE)

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
