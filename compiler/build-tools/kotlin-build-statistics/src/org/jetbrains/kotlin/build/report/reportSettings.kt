/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report

import java.io.File
import java.io.Serializable

data class FileReportSettings(
    val buildReportDir: File,
    val changedFileListPerLimit: Int? = null,
    val includeMetricsInReport: Boolean = false,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 1
    }
}

data class HttpReportSettings(
    val url: String,
    val password: String?,
    val user: String?,
    val verboseEnvironment: Boolean,
    val includeGitBranchName: Boolean
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}