/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Detect memory leaks in annotation processors.
 * @since 2.5.0
 */
@ExperimentalBuildToolsApi
public enum class KaptDetectMemoryLeaksMode {
    /**
     * Default mode.
     */
    DEFAULT,

    /**
     * Paranoid mode.
     */
    PARANOID,

    /**
     * No detection.
     */
    NONE,
}
