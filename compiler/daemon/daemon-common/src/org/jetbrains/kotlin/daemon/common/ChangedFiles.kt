/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.io.Serializable

sealed class ChangedFiles : Serializable {
    sealed class DeterminableFiles : ChangedFiles() {
        class Known(val modified: List<File>, val removed: List<File>, val forDependencies: Boolean = false) : DeterminableFiles()
        class ToBeComputed : DeterminableFiles()
    }

    class Unknown : ChangedFiles()
    companion object {
        const val serialVersionUID: Long = 0
    }
}
