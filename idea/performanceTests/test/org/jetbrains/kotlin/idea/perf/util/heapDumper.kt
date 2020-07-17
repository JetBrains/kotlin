/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import com.intellij.openapi.util.io.FileUtilRt
import com.sun.management.HotSpotDiagnosticMXBean
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.management.ManagementFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HeapDumper {
    private const val HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic"

    private val hotspotMBean = initHotspotMBean()

    private fun initHotspotMBean() = ManagementFactory.newPlatformMXBeanProxy(
        ManagementFactory.getPlatformMBeanServer(),
        HOTSPOT_BEAN_NAME,
        HotSpotDiagnosticMXBean::class.java
    )

    fun dumpHeap(fileNamePrefix: String, live: Boolean = true) {
        val format = SimpleDateFormat("yyyyMMdd-HHmmss")
        val timestamp = format.format(Date())
        val tempFile = File.createTempFile(fileNamePrefix, ".hprof")
        tempFile.delete()
        val fileName = "build/$fileNamePrefix-$timestamp.hprof.zip"
        logMessage { "Dumping a heap into $tempFile ..." }
        try {
            hotspotMBean.dumpHeap(tempFile.toString(), live)
            logMessage { "Heap dump is $tempFile ready." }

            zipFile(tempFile, File(fileName))

            val testName = "Heap dump $timestamp"
            TeamCity.test(testName) {
                TeamCity.artifact(testName, "heapDump", fileName)
            }
        } catch (e: Exception) {
            logMessage { "Error on making a heap dump: ${e.message}" }
            e.printStackTrace()
        }
    }

    private fun zipFile(srcFile: File, targetFile: File) {
        FileInputStream(srcFile).use { fis ->
            ZipOutputStream(FileOutputStream(targetFile)).use { os ->
                os.putNextEntry(ZipEntry(srcFile.name))
                FileUtilRt.copy(fis, os)
                os.closeEntry()
            }
        }
    }
}