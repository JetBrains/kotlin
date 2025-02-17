/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import java.io.File
import java.io.Serializable

/**
 * Represents the changes in source files, which could either be known, to be computed, or unknown.
 *
 * An analogue of [org.jetbrains.kotlin.buildtools.api.SourcesChanges]
 */
sealed class ChangedFiles : Serializable {
    sealed class DeterminableFiles : ChangedFiles() {
        class Known(val modified: List<File>, val removed: List<File>, val forDependencies: Boolean = false) : DeterminableFiles() {
            override fun toString(): String {
                return "Known(modified=$modified, removed=$removed, forDependencies=$forDependencies)"
            }
        }

        object ToBeComputed : DeterminableFiles() {
            @Suppress("unused") // KT-40218
            private fun readResolve(): Any = ToBeComputed
            override fun toString(): String {
                return "ToBeComputed"
            }
        }
    }

    object Unknown : ChangedFiles() {
        @Suppress("unused") // KT-40218
        private fun readResolve(): Any = Unknown
        override fun toString(): String {
            return "Unknown"
        }
    }

    companion object {
        const val serialVersionUID: Long = 1
    }
}