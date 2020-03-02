/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import java.nio.file.*

fun Path.copyRecursively(targetDirectory: Path) {
    val src = this
    Files.walk(src)
        .forEach { source -> Files.copy(source, targetDirectory.resolve(src.relativize(source)), StandardCopyOption.REPLACE_EXISTING) }
}