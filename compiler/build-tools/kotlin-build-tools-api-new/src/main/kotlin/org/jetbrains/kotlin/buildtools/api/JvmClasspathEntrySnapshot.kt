/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.nio.file.Path

public interface JvmClasspathEntrySnapshot {
    public val classSnapshots: Map<String, JvmClassSnapshot>

    public fun saveSnapshot(path: Path)
}

public sealed interface JvmClassSnapshot

public interface InaccessibleJvmClassSnapshot : JvmClassSnapshot

public interface AccessibleJvmClassSnapshot : JvmClassSnapshot {
    public val classAbiHash: Long
}