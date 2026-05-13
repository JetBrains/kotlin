/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

/**
 * Unpacks the JS stdlib KLib into a temporary directory and passes that directory to [action]. The directory is deleted after [action]
 * returns.
 */
internal fun withUnpackedJsStdlib(action: (Path) -> Unit) {
    val klibFile = ForTestCompileRuntime.stdlibJsForTests()
    val tempKlibFolder = Files.createTempDirectory(klibFile.name)

    try {
        ZipFile(klibFile).use { zipFile ->
            for (zipEntry in zipFile.entries()) {
                val targetPath = tempKlibFolder.resolve(zipEntry.name)
                if (zipEntry.isDirectory) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.createDirectories(targetPath.parent)
                    zipFile.getInputStream(zipEntry).use { input ->
                        Files.copy(input, targetPath)
                    }
                }
            }
        }

        action(tempKlibFolder)
    } finally {
        @OptIn(ExperimentalPathApi::class)
        tempKlibFolder.deleteRecursively()
    }
}
