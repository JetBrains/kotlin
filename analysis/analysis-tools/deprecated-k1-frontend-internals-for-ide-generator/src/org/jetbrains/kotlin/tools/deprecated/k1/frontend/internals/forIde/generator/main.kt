/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.deprecated.k1.frontend.internals.forIde.generator

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

fun main() {
    val packages = PackagesToDeprecate.packages
    val rootPath = Paths.get(ROOT_PATH)

    for (packageName in packages) {
        generatePackageJava(packageName, rootPath)
    }
}


fun generatePackageJava(packageName: String, rootPath: Path) {
    val fileContent =
        """
        /**
         * @deprecated Only supported for Kotlin Plugin K1 mode.
         * Use Kotlin Analysis API instead, which works for both K1 and K2 modes.
         * See <a href="https://kotl.in/analysis-api">Kotlin Analysis API documentation</a> and {@link org.jetbrains.kotlin.analysis.api.AnalyzeKt#analyze} for details.
         */
        @Deprecated
        @ApiStatus.ScheduledForRemoval
        package ${packageName};
        
        import org.jetbrains.annotations.ApiStatus;
        """.trimIndent()

    val path = packageNameToDirectoryName(packageName, rootPath)
    path.createDirectories()
    path.resolve("package-info.java").writeText(fileContent)
}

private const val ROOT_PATH =
    "analysis/analysis-tools/deprecated-k1-frontend-internals-for-ide-generated/gen"

private fun packageNameToDirectoryName(packageName: String, rootPath: Path): Path =
    packageName.split(".").fold(rootPath) { path, part -> path.resolve(part) }