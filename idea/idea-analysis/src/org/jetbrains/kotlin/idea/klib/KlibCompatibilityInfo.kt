/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.klib

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.metadataVersion
import org.jetbrains.kotlin.idea.klib.KlibCompatibilityInfo.*
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * Whether a certain KLIB is compatible for the purposes of IDE: indexation, resolve, etc.
 */
sealed class KlibCompatibilityInfo(val isCompatible: Boolean) {
    object Compatible : KlibCompatibilityInfo(true)
    object Pre14Layout : KlibCompatibilityInfo(false)
    class IncompatibleMetadata(val isOlder: Boolean) : KlibCompatibilityInfo(false)
}

fun KotlinLibrary.getCompatibilityInfo(): KlibCompatibilityInfo {
    val hasPre14Manifest = safeRead(false) { has_pre_1_4_manifest }
    if (hasPre14Manifest)
        return Pre14Layout

    val metadataVersion = safeRead(null) { metadataVersion }
    return when {
        metadataVersion == null -> IncompatibleMetadata(true) // too old KLIB format, even doesn't have metadata version
        !metadataVersion.isCompatible() -> IncompatibleMetadata(!metadataVersion.isAtLeast(KlibMetadataVersion.INSTANCE))
        else -> Compatible
    }
}
