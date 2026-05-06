/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util

import java.io.File

fun String.walkRepositoryKotlinFilesWithoutTestData(f: (File) -> Unit) {
    val root = File(this)
    for (file in root.walkTopDown()) {
        if (file.isDirectory) continue
        val path = file.path.lowercase()
        if (
            path.contains("testdata") ||
            path.contains("kotlin-native") ||
            path.contains("resources")
        ) continue

        if (file.extension != "kt") continue

        f(file)
    }
}

fun String.walkRepositoryKotlinFilesWithTestData(f: (File) -> Unit) {
    val root = File(this)
    for (file in root.walkTopDown()) {
        if (file.isDirectory) continue
        if (file.extension != "kt") continue

        f(file)
    }
}
