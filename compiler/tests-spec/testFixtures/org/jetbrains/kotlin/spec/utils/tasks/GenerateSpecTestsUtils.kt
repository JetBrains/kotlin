/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.tasks

import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTS_MAP_FILENAME
import java.io.File
import java.nio.file.Files

// `baseDir` is used in Kotlin plugin from IJ infra
fun detectDirsWithTestsMapFileOnly(dirName: String, baseDir: String = "."): List<String> {
    val excludedDirs = mutableListOf<String>()

    File("${baseDir}/$SPEC_TESTDATA_PATH/$dirName").walkTopDown().forEach { file ->
        val listFiles = Files.walk(file.toPath()).filter(Files::isRegularFile)

        if (file.isDirectory && listFiles?.allMatch { it.endsWith(TESTS_MAP_FILENAME) } == true) {
            val relativePath = file.relativeTo(File("${baseDir}/$SPEC_TESTDATA_PATH/$dirName")).path

            if (!excludedDirs.any { relativePath.startsWith(it) }) {
                excludedDirs.add(relativePath)
            }
        }
    }

    return excludedDirs.sorted().map { it.replace("\\", "/") }
}
