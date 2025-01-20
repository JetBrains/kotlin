/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.nio.file.Path

public interface JvmPlatformToolchain {
    /**
     * Creates a self-contained operation descriptor to be executed by [KotlinToolchain.executeOperation]
     *
     * Basically, converts sources into class files.
     */
    public fun makeJvmCompilationOperation(): JvmCompilationOperation

    public fun calculateClasspathSnapshot(classpathEntry: Path, granularity: JvmClassSnapshotGranularity): JvmClasspathEntrySnapshot
}