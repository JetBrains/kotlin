/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.idea.KotlinFileType

/**
 * Whether the file is parsed as a script: every source file but a `.kt` one is.
 */
fun KtSourceFile.isScript(): Boolean = FileUtilRt.getExtension(name) != KotlinFileType.EXTENSION
