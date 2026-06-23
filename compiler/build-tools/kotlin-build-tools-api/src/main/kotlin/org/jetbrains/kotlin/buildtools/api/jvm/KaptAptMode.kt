/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Annotation processing mode: only apt, only stub generation, both, or with the subsequent compilation.
 * @since 2.5.0
 */
@ExperimentalBuildToolsApi
public enum class KaptAptMode {
    /**
     * Generate stubs and run annotation processing.
     */
    STUBS_AND_APT,

    /**
     * Generate stubs only.
     */
    STUBS_ONLY,

    /**
     * Run annotation processing only.
     */
    APT_ONLY,
}
