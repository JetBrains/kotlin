/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

/**
 * The target platform of the compilation performed by [compile].
 */
@KaExperimentalApi
public enum class KaCompilationTarget {
    /**
     * JVM target (produces '.class' files).
     */
    JVM,
}
