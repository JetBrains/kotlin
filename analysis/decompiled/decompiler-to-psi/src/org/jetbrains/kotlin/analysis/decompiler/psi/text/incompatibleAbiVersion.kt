// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

private const val FILE_ABI_VERSION_MARKER: String = "FILE_ABI"
private const val CURRENT_ABI_VERSION_MARKER: String = "CURRENT_ABI"

const val INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT: String =
    "// This class file was compiled with different version of Kotlin compiler and can't be decompiled."

private const val INCOMPATIBLE_ABI_VERSION_COMMENT: String = "$INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT\n" +
        "//\n" +
        "// Current compiler ABI version is $CURRENT_ABI_VERSION_MARKER\n" +
        "// File ABI version is $FILE_ABI_VERSION_MARKER"

fun <V : BinaryVersion> createIncompatibleAbiVersionDecompiledText(expectedVersion: V, actualVersion: V): DecompiledText = DecompiledText(
    INCOMPATIBLE_ABI_VERSION_COMMENT.replace(CURRENT_ABI_VERSION_MARKER, expectedVersion.toString())
        .replace(FILE_ABI_VERSION_MARKER, actualVersion.toString())
)
