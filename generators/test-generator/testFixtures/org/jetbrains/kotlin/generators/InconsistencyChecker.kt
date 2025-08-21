/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

import java.util.Collections

interface InconsistencyChecker {
    fun add(affectedFile: String)

    val affectedFiles: List<String>

    companion object {
        fun hasDryRunArg(args: Array<String>) = args.any { it == "dryRun" }

        fun inconsistencyChecker(dryRun: Boolean) = if (dryRun) DefaultInconsistencyChecker else EmptyInconsistencyChecker
    }
}

object DefaultInconsistencyChecker : InconsistencyChecker {
    private val files = Collections.synchronizedList(mutableListOf<String>())

    override fun add(affectedFile: String) {
        files.add(affectedFile)
    }

    override val affectedFiles: List<String>
        get() = files
}

object EmptyInconsistencyChecker : InconsistencyChecker {
    override fun add(affectedFile: String) {
    }

    override val affectedFiles: List<String>
        get() = emptyList()
}
