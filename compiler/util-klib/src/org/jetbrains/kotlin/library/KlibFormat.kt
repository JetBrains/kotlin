/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import java.nio.file.Path
import kotlin.io.path.isRegularFile

/**
 * The format of a Klib library.
 */
sealed interface KlibFormat {
    object Directory : KlibFormat
    object ZipArchive : KlibFormat

    companion object {
        /**
         * This function is made internal because it's semantics can be changed in the future.
         */
        internal fun guessBy(path: Path): KlibFormat {
            return if (path.isRegularFile()) ZipArchive else Directory
        }
    }
}
