/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile

/**
 * The file represents a decompiled .knm file
 *
 * @see KlibMetaFileType
 */
class KlibDecompiledFile(provider: KotlinDecompiledFileViewProvider) : KtDecompiledFile(provider)
