/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import java.io.File
import java.nio.file.Path

/**
 * Returns a canonical path computed the same way as [File.canonicalPath] does.
 */
fun Path.canonicalPathString(): String = toFile().canonicalPath
