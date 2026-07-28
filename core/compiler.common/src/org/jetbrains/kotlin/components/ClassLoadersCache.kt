/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.components

import java.io.File
import java.net.URLClassLoader

/**
 * Cache for classloaders to be used by compiler plugins that need to load additional classes dynamically.
 *
 * May be passed into compiler `Services` to be used by compiler plugins.
 * A compiler plugin must add explicit support for using the `ClassLoadersCache` to benefit from it.
 */
interface ClassLoadersCache {
    fun getForClassPath(files: List<File>): ClassLoader
}
