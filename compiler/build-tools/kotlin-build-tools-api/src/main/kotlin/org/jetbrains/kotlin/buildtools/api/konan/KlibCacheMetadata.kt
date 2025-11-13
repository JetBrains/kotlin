/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import java.nio.file.Path

/**
 * to be used for creating build operations via [NativePlatformToolchain.createBuildCacheOperation]
 */
public interface KlibCacheMetadata {
    public val klib: Path

    /**
     * Caches of each dependency klib
     */
    public val requiredKlibCaches: List<Path>

    public val isDirty: Boolean
}