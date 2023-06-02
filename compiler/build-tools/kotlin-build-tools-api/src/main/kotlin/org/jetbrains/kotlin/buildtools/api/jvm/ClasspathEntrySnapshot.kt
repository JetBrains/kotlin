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
    public val classSnapshots: LinkedHashMap<String, ClassSnapshot>

    public fun saveSnapshot(path: File)
}

/**
 * TODO add docs KT-57565
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public interface ClassSnapshot {
    // ... TODO: KT-57565, it will expose some part of org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot hierarchy
}