/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import java.io.File

/**
 * TODO add docs KT-57565
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public interface ClasspathEntrySnapshot {
    public val classSnapshots: Map<String, ClassSnapshot>

    public fun saveSnapshot(path: File)
}

/**
 * TODO add docs KT-57565
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public sealed interface ClassSnapshot

/**
 * [ClassSnapshot] of an inaccessible class.
 *
 * A class is inaccessible if it can't be referenced from other source files (and therefore any changes in an inaccessible class will not
 * require recompilation of other source files).
 */
@ExperimentalBuildToolsApi
public interface InaccessibleClassSnapshot : ClassSnapshot

@ExperimentalBuildToolsApi
public interface AccessibleClassSnapshot : ClassSnapshot {
    public val classAbiHash: Long
}