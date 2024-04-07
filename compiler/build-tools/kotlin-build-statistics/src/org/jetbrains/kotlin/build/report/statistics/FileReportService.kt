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
    private val buildReportDir: File,
    private val projectName: String,
    private val fileSuffix: String,
) : BuildReportService<T> {
    companion object {
        internal val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").also { it.timeZone = TimeZone.getTimeZone("UTC") }
        private const val MAX_ATTEMPTS = 10
    }

    private val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)

    abstract fun printBuildReport(data: T, outputFile: File)

    override fun process(data: T, log: KotlinLogger) {
        try {
            buildReportDir.mkdirs()
            if (!(buildReportDir.exists() && buildReportDir.isDirectory)) {
                log.error("Kotlin build report cannot be created: '$buildReportDir' is a file or do not have permissions to create")
                return
            }

            val outputFile = createReportFile(log)
            if (outputFile != null) {
                val buildReportPath = outputFile.toPath().toUri().toString()
                printBuildReport(data, outputFile)

                log.lifecycle("Kotlin build report is written to $buildReportPath")
            }
        } catch (e: Exception) {
            log.error("Could not create Kotlin build report in $buildReportDir", e)
        }
    }

    private fun createReportFile(log: KotlinLogger): File? {
        for (index in 0..MAX_ATTEMPTS) {
            val outputFile = buildReportDir.resolve("$projectName-build-$ts-$index.$fileSuffix")
            if (outputFile.createNewFile()) {
                return outputFile
            }
        }

        log.error("Failed to create report file after $MAX_ATTEMPTS attempts")
        return null
    }
}