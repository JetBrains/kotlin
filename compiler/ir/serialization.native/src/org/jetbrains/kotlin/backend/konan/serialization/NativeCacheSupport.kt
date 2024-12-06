/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.PartialCacheInfoBase

class NativeCacheSupport(
    val cachedLibraries: CachedLibrariesBase,
    val lazyIrForCaches: Boolean,
    val libraryBeingCached: PartialCacheInfoBase?,
)
