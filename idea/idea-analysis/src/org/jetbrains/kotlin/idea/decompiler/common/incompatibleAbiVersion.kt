/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler.common

import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledTextIndex
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

private val FILE_ABI_VERSION_MARKER: String = "FILE_ABI"
private val CURRENT_ABI_VERSION_MARKER: String = "CURRENT_ABI"

val INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT: String = "// This class file was compiled with different version of Kotlin compiler and can't be decompiled."
private val INCOMPATIBLE_ABI_VERSION_COMMENT: String =
        "$INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT\n" +
        "//\n" +
        "// Current compiler ABI version is $CURRENT_ABI_VERSION_MARKER\n" +
        "// File ABI version is $FILE_ABI_VERSION_MARKER"

fun <V : BinaryVersion> createIncompatibleAbiVersionDecompiledText(expectedVersion: V, actualVersion: V): DecompiledText {
    return DecompiledText(
            INCOMPATIBLE_ABI_VERSION_COMMENT
                    .replace(CURRENT_ABI_VERSION_MARKER, expectedVersion.toString())
                    .replace(FILE_ABI_VERSION_MARKER, actualVersion.toString()),
            DecompiledTextIndex.Empty
    )
}
