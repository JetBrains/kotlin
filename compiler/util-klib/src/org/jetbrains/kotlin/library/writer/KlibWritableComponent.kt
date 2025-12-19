/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.writer

import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * A special "writable" component of a constructed Klib library. This component needs to be registered in [KlibWriter.components].
 *
 * The only contract of this component is to implement [writeTo], which accepts the root directory of the constructed library,
 * and is responsible for writing all the necessary files to the library.
 */
interface KlibWritableComponent {
    fun writeTo(root: KlibFile)
}
