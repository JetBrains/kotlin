/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.deprecated.k1.frontend.internals.forIde.generator

import java.nio.file.Paths
import kotlin.io.path.readLines

object PackagesToDeprecate {
    val packages: Set<String> by lazy {
        Paths.get(EXPERIMENTAL_ANNOTATIONS_PATH)
            .readLines()
            .map { it.trim() }
            .filterNot { it.startsWith("#") || it.isBlank() }
            .toSet()
    }

    private const val EXPERIMENTAL_ANNOTATIONS_PATH =
        "analysis/analysis-tools/deprecated-k1-frontend-internals-for-ide-generator/PackagesToDeprecate.txt"
}