/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowExitPointSnapshot
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowInfoProvider
import org.jetbrains.kotlin.psi.KtExpression

@OptIn(KtAnalysisNonPublicApi::class)
internal class KtFe10DataFlowInfoProvider(override val analysisSession: KtAnalysisSession) : KtDataFlowInfoProvider() {
    override fun getExitPointSnapshot(statements: List<KtExpression>): KtDataFlowExitPointSnapshot {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}