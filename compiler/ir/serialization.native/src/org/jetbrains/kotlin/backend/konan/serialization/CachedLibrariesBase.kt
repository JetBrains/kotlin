/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.library.KotlinLibrary

interface CachedLibrariesBase {
    fun isLibraryCached(library: KotlinLibrary): Boolean
    fun getLibraryCache(library: KotlinLibrary, allowIncomplete: Boolean = false): Cache?

    enum class Kind { DYNAMIC, STATIC, HEADER }
}