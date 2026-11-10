/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.name.Name

/**
 * Must not depend on the IntelliJ platform.
 */
@NoMutableState
object FirKmpSyntaxComponent : FirSyntaxComponent<Any>() {
    override val parserMode: ParserMode
        get() = KmpTree

    override fun KtSourceElement.unwrap(): Any {
        notYetImplemented()
    }

    override fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement> {
        notYetImplemented()
    }

    override fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement? {
        notYetImplemented()
    }

    override fun KtSourceElement.findDefaultValueSource(): KtSourceElement? {
        notYetImplemented()
    }

    override fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement? {
        notYetImplemented()
    }

    override fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement? {
        notYetImplemented()
    }

    override fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement? {
        notYetImplemented()
    }

    override fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int? {
        notYetImplemented()
    }

    override fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean {
        notYetImplemented()
    }

    private fun notYetImplemented(): Nothing = TODO("Not yet implemented: KT-91322")
}
