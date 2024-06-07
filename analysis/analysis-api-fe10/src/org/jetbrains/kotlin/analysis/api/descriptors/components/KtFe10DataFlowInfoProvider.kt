/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaDataFlowExitPointSnapshot
import org.jetbrains.kotlin.analysis.api.components.KaDataFlowProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression

@OptIn(KaAnalysisNonPublicApi::class)
internal class KaFe10DataFlowProvider(
    override val analysisSessionProvider: () -> KaSession,
    override val token: KaLifetimeToken
) : KaSessionComponent<KaSession>(), KaDataFlowProvider {
    override fun computeExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot = withValidityAssertion {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}