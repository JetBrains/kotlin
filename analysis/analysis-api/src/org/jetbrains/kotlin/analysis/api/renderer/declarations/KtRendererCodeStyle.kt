/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType

public interface KtRendererCodeStyle {
    context(KtAnalysisSession)
    public fun getIndentSize(): Int

    context(KtAnalysisSession)
    public fun getSeparatorAfterContextReceivers(): String

    context(KtAnalysisSession)
    public fun getSeparatorBetweenAnnotationAndOwner(symbol: KtAnnotated): String

    context(KtAnalysisSession)
    public fun getSeparatorBetweenAnnotations(symbol: KtAnnotated): String

    context(KtAnalysisSession)
    public fun getSeparatorBetweenModifiers(): String

    context(KtAnalysisSession)
    public fun getSeparatorBetweenMembers(first: KtDeclarationSymbol, second: KtDeclarationSymbol): String
}

public object KtRecommendedRendererCodeStyle : KtRendererCodeStyle {
    context(KtAnalysisSession)
    override fun getIndentSize(): Int = 4

    context(KtAnalysisSession)
    override fun getSeparatorAfterContextReceivers(): String = "\n"

    context(KtAnalysisSession)
    override fun getSeparatorBetweenAnnotationAndOwner(symbol: KtAnnotated): String {
        return when (symbol) {
            is KtType -> " "
            is KtTypeParameterSymbol -> " "
            is KtValueParameterSymbol -> " "
            else -> "\n"
        }
    }

    context(KtAnalysisSession)
    override fun getSeparatorBetweenAnnotations(symbol: KtAnnotated): String {
        return when (symbol) {
            is KtType -> " "
            is KtTypeParameterSymbol -> " "
            is KtValueParameterSymbol -> " "
            else -> "\n"
        }
    }

    context(KtAnalysisSession)
    override fun getSeparatorBetweenModifiers(): String = " "

    context(KtAnalysisSession)
    override fun getSeparatorBetweenMembers(first: KtDeclarationSymbol, second: KtDeclarationSymbol): String {
        return when {
            first is KtEnumEntrySymbol && second is KtEnumEntrySymbol -> ",\n"
            first is KtEnumEntrySymbol && second !is KtEnumEntrySymbol -> ";\n\n"
            else -> "\n\n"
        }
    }

}