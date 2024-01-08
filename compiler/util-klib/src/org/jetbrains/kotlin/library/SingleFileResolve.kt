/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

fun interface SingleFileKlibResolveStrategy {
    fun resolve(libraryFile: File, logger: Logger): KotlinLibrary
}

fun resolveSingleFileKlib(
    libraryFile: File,
    logger: Logger = DummyLogger,
    strategy: SingleFileKlibResolveStrategy = CompilerSingleFileKlibResolveStrategy
): KotlinLibrary = strategy.resolve(libraryFile, logger)
