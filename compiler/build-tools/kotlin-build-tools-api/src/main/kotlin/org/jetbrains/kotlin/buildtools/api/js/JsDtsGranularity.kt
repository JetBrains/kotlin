/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * The granularity of TypeScript declaration (`.d.ts`) generation.
 *
 * Must be kept consistent with the JS linking granularity (the `-Xir-per-module` / `-Xir-per-file`
 * compiler flags)
 *
 * @since 2.5.0
 */
@ExperimentalBuildToolsApi
public enum class JsDtsGranularity {
    /**
     * A single set of declarations for the whole dependency graph ("closed world").
     */
    WHOLE_PROGRAM,

    /**
     * One set of declarations per module.
     */
    PER_MODULE,

    /**
     * One set of declarations per source file.
     */
    PER_FILE,
}
