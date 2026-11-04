/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.fir.FirSyntaxComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.name.Name

/**
 * [FirSyntaxComponent] for sessions whose sources are built by [MultiplatformParsing2Fir].
 *
 * Must not depend on the IntelliJ platform.
 */
@NoMutableState
object FirKmpSyntaxComponent : FirSyntaxComponent() {
    override val parserMode: ParserMode
        get() = ParserMode.KmpTree

    override fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement> {
        TODO("Not yet implemented")
    }

    override fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement? {
        TODO("Not yet implemented")
    }

    override fun KtSourceElement.findDefaultValueSource(): KtSourceElement? {
        TODO("Not yet implemented")
    }

    override fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement? {
        TODO("Not yet implemented")
    }

    override fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement? {
        TODO("Not yet implemented")
    }

    override fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement? {
        TODO("Not yet implemented")
    }

    override fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int? {
        TODO("Not yet implemented")
    }

    override fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean {
        TODO("Not yet implemented")
    }
}
