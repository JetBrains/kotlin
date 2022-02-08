/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ObsoleteTestInfrastructure::class)
internal fun runJvmInstance(
    jdkHome: File,
    additionalArgs: List<String>,
    classPath: List<File>,
    classNameToRun: String,
) {
    val javaExe = File(jdkHome, "bin/java.exe").takeIf(File::exists)
        ?: File(jdkHome, "bin/java").takeIf(File::exists)
        ?: error("Can't find 'java' executable in $jdkHome")

    val command = arrayOf(
        javaExe.absolutePath,
        "-ea",
        *additionalArgs.toTypedArray(),
        "-classpath",
        classPath.joinToString(File.pathSeparator, transform = File::getAbsolutePath),
        classNameToRun,
    )

    val process = ProcessBuilder(*command).inheritIO().start()
    process.waitFor(1, TimeUnit.MINUTES)
    process.outputStream.flush()
    AbstractBlackBoxCodegenTest.assertEquals(0, process.exitValue())
}
