/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.junit.Test
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.test.assertEquals

class FirMetaModularizedTest {

    private fun List<String>.filterArguments() = filterNot { it.startsWith("-Djava.security.manager") }

    @Test
    fun doTest() {
        val runtimeBean = ManagementFactory.getRuntimeMXBean()
        val jvmCommand = System.getProperty("java.home") + "/bin/java"

        val runCount = System.getProperty("fir.bench.multirun.count").toInt()


        val startTimestamp = System.currentTimeMillis()

        for (i in 0 until runCount) {
            val pb = ProcessBuilder()
                .inheritIO()
                .command(
                    jvmCommand, "-cp", runtimeBean.classPath,
                    *runtimeBean.inputArguments.filterArguments().toTypedArray(),
                    "-Dfir.bench.report.timestamp=$startTimestamp",
                    StandaloneModularizedTestRunner::class.java.canonicalName
                )
                .directory(File("").absoluteFile)

            val process = pb.start()

            assertEquals(0, process.waitFor(), "Forked test should complete normally")
        }
    }
}