/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.KaEvaluator
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.analysis.api.evaluation.evaluate as evaluateEndpoint
import org.jetbrains.kotlin.analysis.api.evaluation.evaluateAsAnnotationValue as evaluateAsAnnotationValueEndpoint

internal class KaEvaluatorBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaEvaluator {
    override fun KtExpression.evaluate(): KaConstantValue? =
        context(analysisSession) { evaluateEndpoint() }

    override fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue? =
        context(analysisSession) { evaluateAsAnnotationValueEndpoint() }
}
