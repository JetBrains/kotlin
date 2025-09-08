/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.js

import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder

class KotlinJavaScriptMetaFileDecompiler : KotlinMetadataDecompiler() {
    override fun getStubBuilder(): KotlinMetadataStubBuilder = KotlinJavaScriptMetadataStubBuilder
    override fun createFile(viewProvider: KotlinDecompiledFileViewProvider): KtDecompiledFile = KjsmDecompiledFile(viewProvider)
}

