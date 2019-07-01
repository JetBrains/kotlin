/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import java.io.Serializable

interface CompilerServicesFacadeBaseAsync {
    /**
     * Reports different kind of diagnostic messages from compile daemon to compile daemon clients (jps, gradle, ...)
     */
    suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?)
}

suspend fun CompilerServicesFacadeBaseAsync.report(
        category: ReportCategory,
        severity: ReportSeverity,
        message: String? = null,
        attachment: Serializable? = null
) {
    report(category.code, severity.code, message, attachment)
}

