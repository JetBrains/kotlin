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
    public fun getIndentSize(analysisSession: KtAnalysisSession): Int

    public fun getSeparatorAfterContextReceivers(analysisSession: KtAnalysisSession): String

    public fun getSeparatorBetweenAnnotationAndOwner(analysisSession: KtAnalysisSession, symbol: KtAnnotated): String

    public fun getSeparatorBetweenAnnotations(analysisSession: KtAnalysisSession, symbol: KtAnnotated): String

    public fun getSeparatorBetweenModifiers(analysisSession: KtAnalysisSession): String

    public fun getSeparatorBetweenMembers(
        analysisSession: KtAnalysisSession,
        first: KtDeclarationSymbol,
        second: KtDeclarationSymbol,
    ): String
}

public object KtRecommendedRendererCodeStyle : KtRendererCodeStyle {
    override fun getIndentSize(analysisSession: KtAnalysisSession): Int = 4

    override fun getSeparatorAfterContextReceivers(analysisSession: KtAnalysisSession): String = "\n"

    override fun getSeparatorBetweenAnnotationAndOwner(analysisSession: KtAnalysisSession, symbol: KtAnnotated): String {
        return when (symbol) {
            is KtType -> " "
            is KtTypeParameterSymbol -> " "
            is KtValueParameterSymbol -> " "
            else -> "\n"
        }
    }

    override fun getSeparatorBetweenAnnotations(analysisSession: KtAnalysisSession, symbol: KtAnnotated): String {
        return when (symbol) {
            is KtType -> " "
            is KtTypeParameterSymbol -> " "
            is KtValueParameterSymbol -> " "
            else -> "\n"
        }
    }

    override fun getSeparatorBetweenModifiers(analysisSession: KtAnalysisSession): String = " "

    override fun getSeparatorBetweenMembers(analysisSession: KtAnalysisSession, first: KtDeclarationSymbol, second: KtDeclarationSymbol): String {
        return when {
            first is KtEnumEntrySymbol && second is KtEnumEntrySymbol -> ",\n"
            first is KtEnumEntrySymbol && second !is KtEnumEntrySymbol -> ";\n\n"
            else -> "\n\n"
        }
    }

}