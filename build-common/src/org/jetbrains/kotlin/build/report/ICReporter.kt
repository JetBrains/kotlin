/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report

import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.File

interface ICReporter {
    fun report(message: () -> String)
    fun reportVerbose(message: () -> String)

    fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode)
    fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String)
    fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String)
    fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String)
}


