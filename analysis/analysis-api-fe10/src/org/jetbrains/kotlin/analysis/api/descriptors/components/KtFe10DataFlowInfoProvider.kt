/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaDataFlowExitPointSnapshot
import org.jetbrains.kotlin.analysis.api.components.KaDataFlowInfoProvider
import org.jetbrains.kotlin.psi.KtExpression

@OptIn(KaAnalysisNonPublicApi::class)
internal class KaFe10DataFlowInfoProvider(override val analysisSession: KaSession) : KaDataFlowInfoProvider() {
    override fun getExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}