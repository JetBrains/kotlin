/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.konan.fir.test.cases.session.builder

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompilerWithoutOutputInterceptor
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.streams.asSequence

internal fun testDataPath(path: String): Path {
    return Paths.get("analysis/analysis-api-standalone/analysis-api-standalone-native/testData/nativeSessionBuilder").resolve(path)
}

internal fun compileToNativeKLib(kLibSourcesRoot: Path): Path {
    val ktFiles = Files.walk(kLibSourcesRoot).asSequence().filter { it.extension == "kt" }.toList()
    val testKlib = KtTestUtil.tmpDir("testLibrary").resolve("library.klib").toPath()

    val arguments = buildList {
        ktFiles.mapTo(this) { it.absolutePathString() }
        addAll(listOf("-produce", "library"))
        addAll(listOf("-output", testKlib.absolutePathString()))
    }

    val compileResult = callCompilerWithoutOutputInterceptor(arguments.toTypedArray())

    check(compileResult.exitCode == ExitCode.OK) {
        "Compilation error: $compileResult"
    }

    return testKlib
}