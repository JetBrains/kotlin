/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

fun String.walkTopDown(f: (Path) -> Unit) {
    val root = Path(this)
    for (file in root.walk()) {
        if (file.isDirectory()) continue
        val path = file.pathString
        if (
            path.contains("kotlin-native") ||
            path.contains("testData") ||
            path.contains("testdata") ||
            path.contains("resources")
        ) continue

        if (path.replace(File.separator, "/").contains("stdlib/api")) continue

        if (file.extension != "kt") continue

        f(file)
    }
}

fun String.walkTopDownWithTestData(f: (Path) -> Unit) {
    val root = Path(this)
    for (file in root.walk()) {
        if (file.isDirectory()) continue
        if (file.extension != "kt") continue

        f(file)
    }
}
