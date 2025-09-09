/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile

/**
 * The file represents decompiled .kotlin_builtins/.kotlin_metadata files
 *
 * @see KotlinBuiltInFileType
 */
class KotlinBuiltinsDecompiledFile(viewProvider: KotlinDecompiledFileViewProvider) : KtDecompiledFile(viewProvider)
