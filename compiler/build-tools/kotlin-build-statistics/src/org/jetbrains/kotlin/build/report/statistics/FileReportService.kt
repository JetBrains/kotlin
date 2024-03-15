/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

abstract class FileReportService<T>(
    buildReportDir: File,
    projectName: String,
    fileSuffix: String,
) : BuildReportService<T> {
    companion object {
        internal val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").also { it.timeZone = TimeZone.getTimeZone("UTC") }
    }

    private val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)
    private val outputFile = buildReportDir.resolve("$projectName-build-$ts.$fileSuffix")

    abstract fun printBuildReport(data: T, outputFile: File)

    override fun process(data: T, log: KotlinLogger) {
        val buildReportPath = outputFile.toPath().toUri().toString()
        try {
            outputFile.parentFile.mkdirs()
            if (!(outputFile.parentFile.exists() && outputFile.parentFile.isDirectory)) {
                log.error("Kotlin build report cannot be created: '${outputFile.parentFile}' is a file or do not have permissions to create")
                return
            }
            printBuildReport(data, outputFile)

            log.lifecycle("Kotlin build report is written to $buildReportPath")
        } catch (e: Exception) {
            log.error("Could not write Kotlin build report to $buildReportPath", e)
        }
    }
}