/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession

internal interface KtFirAnalysisSessionComponent {
    val analysisSession: KtFirAnalysisSession

    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveState get() = analysisSession.firResolveState
    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.buildKtType(this)
}