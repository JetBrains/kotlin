/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.io.File
import java.io.Serializable

/**
 * A hierarchy representing source files changes for incremental compilation
 */
public sealed interface SourcesChanges : Serializable {
    /**
     * A marker object stating that the API consumer cannot calculate changes (either because it's an initial build or for some other reason).
     * The Build Tools API will not enable its source file changes detector in this mode, expecting the API consumer to provide file changes as [Known] for the consequent builds.
     */
    public object Unknown : SourcesChanges

    /**
     * A marker object stating that the API consumer is not capable of calculating source file changes.
     * In this mode, the Build Tools API will enable its source file changes detector and detect changes itself.
     */
    public object ToBeCalculated : SourcesChanges

    /**
     * A class containing [modifiedFiles] and [removedFiles] calculated from source file changes by the API consumer.
     */
    public class Known(
        public val modifiedFiles: List<File>,
        public val removedFiles: List<File>,
    ) : SourcesChanges
}