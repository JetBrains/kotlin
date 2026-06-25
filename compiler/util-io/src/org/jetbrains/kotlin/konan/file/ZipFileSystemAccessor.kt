/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

@Deprecated(
    "Preserved for binary compatibility with existing versions of the kotlinx-benchmarks Gradle plugin. See KT-82882." +
            "\nPlease use KlibLoader.load() instead.",
    replaceWith = ReplaceWith("KlibLoader.load()", imports = ["org.jetbrains.kotlin.library.loader.KlibLoader"]),
    level = DeprecationLevel.ERROR
)
interface ZipFileSystemAccessor

