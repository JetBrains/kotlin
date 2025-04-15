/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import java.io.File

internal class PreKotlin_2_1_21_Wrapper(
    private val base: CompilationService
) : CompilationService by base {
    override fun calculateClasspathSnapshot(
        classpathEntry: File,
        granularity: ClassSnapshotGranularity,
        parseInlinedLocalClasses: Boolean
    ): ClasspathEntrySnapshot {
        // the parseInlinedLocalClasses api wasn't available before
        return base.calculateClasspathSnapshot(classpathEntry, granularity)
    }
}
