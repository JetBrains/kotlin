/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

fun interface SingleFileKlibResolveStrategy {
    fun resolve(libraryFile: File, logger: Logger): KotlinLibrary
}

@Deprecated(
    "Preserved for binary compatibility with existing versions of the kotlinx-benchmarks Gradle plugin. See KT-82882.",
    level = DeprecationLevel.HIDDEN
)
fun resolveSingleFileKlib(
    libraryFile: File,
    logger: Logger = DummyLogger,
    strategy: SingleFileKlibResolveStrategy = SingleFileKlibResolveStrategy { _, _ ->
        val klibLoadingResult = KlibLoader { libraryPaths(libraryFile.path) }.load()
        klibLoadingResult.reportLoadingProblemsIfAny { _, message ->
            // N.B. A call to the @Deprecated Logger.fatal() function is intentional here.
            // It's necessary to replicate the behavior that existed before.
            @Suppress("DEPRECATION") logger.fatal(message)
        }
        klibLoadingResult.librariesStdlibFirst.single()
    }
): KotlinLibrary = strategy.resolve(libraryFile, logger)
