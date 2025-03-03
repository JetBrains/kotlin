/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

private const val FILE_METADATA_VERSION_MARKER: String = "FILE_METADATA"
private const val CURRENT_METADATA_VERSION_MARKER: String = "CURRENT_METADATA"

const val INCOMPATIBLE_METADATA_VERSION_GENERAL_COMMENT: String =
    "// This file was compiled with different version of Kotlin compiler and can't be decompiled."

private const val INCOMPATIBLE_METADATA_VERSION_COMMENT: String = "$INCOMPATIBLE_METADATA_VERSION_GENERAL_COMMENT\n" +
        "//\n" +
        "// Current compiler can accept metadata with versions $CURRENT_METADATA_VERSION_MARKER or lower\n" +
        "// File metadata version is $FILE_METADATA_VERSION_MARKER"

fun <V : BinaryVersion> createIncompatibleMetadataVersionDecompiledText(expectedVersion: V, actualVersion: V): DecompiledText = DecompiledText(
    INCOMPATIBLE_METADATA_VERSION_COMMENT.replace(CURRENT_METADATA_VERSION_MARKER, expectedVersion.toString())
        .replace(FILE_METADATA_VERSION_MARKER, actualVersion.toString())
)
