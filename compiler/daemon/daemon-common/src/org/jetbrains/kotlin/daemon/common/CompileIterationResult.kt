/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.io.Serializable

class CompileIterationResult(
    @Suppress("unused") // used in Gradle
        val sourceFiles: Iterable<File>,
    @Suppress("unused") // used in Gradle
        val exitCode: String
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}