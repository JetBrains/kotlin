/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirScriptCodeFragmentBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirSnippetBuilder

interface FirScriptCodeFragmentExtension<in Builder: FirScriptCodeFragmentBuilder> {
    fun Builder.configureContainingFile(fileBuilder: FirFileBuilder)
    fun Builder.configure(sourceFile: KtSourceFile)
}
