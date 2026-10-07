/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

/**
 * ToleratedErrorsTracker collects source files which had compilation errors tolerated by the error-tolerant compilation mode
 * (i.e., the code with errors was compiled into `throw`s instead of failing the compilation).
 *
 * Incremental compilation uses it to recompile such files in the next build even if they are not changed:
 * the errors might disappear after their dependencies are fixed.
 */
interface ToleratedErrorsTracker {
    /**
     * Report that the source file [filePath] contains a tolerated compilation error.
     */
    fun report(filePath: String)

    object DoNothing : ToleratedErrorsTracker {
        override fun report(filePath: String) {
        }
    }
}
