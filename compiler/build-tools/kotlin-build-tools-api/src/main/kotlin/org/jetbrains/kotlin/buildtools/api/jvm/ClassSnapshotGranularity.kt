/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

/**
 * The granularity of a [ClassSnapshot].
 *
 * There are currently two granularity levels:
 *   - [CLASS_LEVEL]) (coarse-grained): The size of the snapshot will be smaller, but we will have coarse-grained classpath changes, which
 *     means more source files will be recompiled.
 *   - [CLASS_MEMBER_LEVEL] (fine-grained): The size of the snapshot will be larger, but we will have fine-grained classpath changes, which
 *     means fewer source files will be recompiled.
 *
 * Therefore, [CLASS_LEVEL] is typically suitable for classes that are infrequently changed (e.g., external libraries), whereas
 * [CLASS_MEMBER_LEVEL] is suitable for classes that are frequently changed (e.g., classes produced by the current project).
 */
public enum class ClassSnapshotGranularity {

    /**
     * Snapshotting level that allows tracking whether a .class file has changed without tracking what specific parts of the .class file
     * (e.g., fields or methods) have changed.
     */
    CLASS_LEVEL,

    /**
     * Snapshotting level that allows tracking not only whether a .class file has changed but also what specific parts of the .class file
     * (e.g., fields or methods) have changed.
     */
    CLASS_MEMBER_LEVEL
}