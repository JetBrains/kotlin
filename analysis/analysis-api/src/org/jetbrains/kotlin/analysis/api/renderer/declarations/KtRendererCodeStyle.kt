/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

public interface KaRendererCodeStyle {
    public fun getIndentSize(analysisSession: KaSession): Int

    public fun getSeparatorAfterContextReceivers(analysisSession: KaSession): String

    public fun getSeparatorBetweenAnnotationAndOwner(analysisSession: KaSession, symbol: KaAnnotated): String

    public fun getSeparatorBetweenAnnotations(analysisSession: KaSession, symbol: KaAnnotated): String

    public fun getSeparatorBetweenModifiers(analysisSession: KaSession): String

    public fun getSeparatorBetweenMembers(
        analysisSession: KaSession,
        first: KaDeclarationSymbol,
        second: KaDeclarationSymbol,
    ): String
}

public typealias KtRendererCodeStyle = KaRendererCodeStyle

public object KaRecommendedRendererCodeStyle : KaRendererCodeStyle {
    override fun getIndentSize(analysisSession: KaSession): Int = 4

    override fun getSeparatorAfterContextReceivers(analysisSession: KaSession): String = "\n"

    override fun getSeparatorBetweenAnnotationAndOwner(analysisSession: KaSession, symbol: KaAnnotated): String {
        return when (symbol) {
            is KaType -> " "
            is KaTypeParameterSymbol -> " "
            is KaValueParameterSymbol -> " "
            else -> "\n"
        }
    }

    override fun getSeparatorBetweenAnnotations(analysisSession: KaSession, symbol: KaAnnotated): String {
        return when (symbol) {
            is KaType -> " "
            is KaTypeParameterSymbol -> " "
            is KaValueParameterSymbol -> " "
            else -> "\n"
        }
    }

    override fun getSeparatorBetweenModifiers(analysisSession: KaSession): String = " "

    override fun getSeparatorBetweenMembers(analysisSession: KaSession, first: KaDeclarationSymbol, second: KaDeclarationSymbol): String {
        return when {
            first is KaEnumEntrySymbol && second is KaEnumEntrySymbol -> ",\n"
            first is KaEnumEntrySymbol && second !is KaEnumEntrySymbol -> ";\n\n"
            else -> "\n\n"
        }
    }
}

public typealias KtRecommendedRendererCodeStyle = KaRecommendedRendererCodeStyle