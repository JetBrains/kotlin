/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.writer

import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * A "writer" of a specific component of the constructed Klib library.
 * Every instance of [KlibComponentWriter] needs to be registered in [KlibWriter.componentWriters].
 *
 * The only contract of [KlibComponentWriter] is to implement [writeTo], which accepts the root directory of the constructed library,
 * and is responsible for writing all the necessary files to the library.
 */
interface KlibComponentWriter {
    fun writeTo(root: KlibFile)
}
