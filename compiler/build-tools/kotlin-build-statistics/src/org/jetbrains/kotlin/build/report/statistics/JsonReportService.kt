/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics

import com.google.gson.Gson
import java.io.File

class JsonReportService(
    buildReportDir: File,
    projectName: String,
) : FileReportService<Any>(buildReportDir, projectName, "json") {

    /**
     * Prints general build information and task/transform build metrics
     */
    override fun printBuildReport(data: Any, outputFile: File) {
        outputFile.bufferedWriter().use {
            it.write(Gson().toJson(data))
        }
    }
}