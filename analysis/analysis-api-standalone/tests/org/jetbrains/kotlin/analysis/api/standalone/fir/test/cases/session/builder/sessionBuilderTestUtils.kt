/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.test.KlibTestUtil
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.streams.asSequence

internal fun testDataPath(path: String): Path {
    return Paths.get("analysis/analysis-api-standalone/testData/sessionBuilder").resolve(path)
}


internal fun compileCommonKlib(kLibSourcesRoot: Path): Path {
    Files.walk(kLibSourcesRoot)
    val ktFiles = Files.walk(kLibSourcesRoot).asSequence().filter { it.extension == "kt" }.toList()
    val testKlib = KtTestUtil.tmpDir("testLibrary").resolve("library.klib").toPath()
    KlibTestUtil.compileCommonSourcesToKlib(
        ktFiles.map(Path::toFile),
        libraryName = "library",
        testKlib.toFile(),
        libraryVersioning = KotlinLibraryVersioning(
            libraryVersion = null,
            compilerVersion = null,
            abiVersion = KotlinAbiVersion.CURRENT,
            metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
        )
    )

    return testKlib
}