/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import org.junit.Test
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.test.assertEquals

class FirMetaModularizedTest {

    private fun List<String>.filterArguments() = filterNot { it.startsWith("-Djava.security.manager") }

    @Test
    fun doTest() {
        val runtimeBean = ManagementFactory.getRuntimeMXBean()
        val javaExePath = when {
            SystemInfo.isWindows -> "\\bin\\java.exe"
            else -> "/bin/java"
        }
        val jvmCommand = System.getProperty("java.home") + javaExePath

        val runCount = System.getProperty("fir.bench.multirun.count").toInt()


        val startTimestamp = System.currentTimeMillis()
        val file = FileUtil.createTempFile("classpath_container", ".jar")
        file.deleteOnExit()
        val manifest = Manifest()
        manifest.mainAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0")
        manifest.mainAttributes.putValue(Attributes.Name.MAIN_CLASS.toString(), StandaloneModularizedTestRunner::class.java.canonicalName)
        manifest.mainAttributes.putValue(
            Attributes.Name.CLASS_PATH.toString(),
            runtimeBean.classPath.split(File.pathSeparator).joinToString(" ") {
                val f = File(it)
                f.toURI().toString()
            }
        )


        JarOutputStream(file.outputStream(), manifest).close()


        for (i in 0 until runCount) {
            val pb = ProcessBuilder()
                .inheritIO()
                .command(
                    jvmCommand,
                    *runtimeBean.inputArguments.filterArguments().toTypedArray(),
                    "-Dfir.bench.report.timestamp=$startTimestamp",
                    "-jar", file.toString()
                )
                .directory(File("").absoluteFile)

            val process = pb.start()

            assertEquals(0, process.waitFor(), "Forked test should complete normally")
        }
    }
}