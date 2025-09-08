/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.KaEvaluator
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKaAnnotationValue
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValueOrNull
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils

internal class KaFe10Evaluator(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaEvaluator, KaFe10SessionComponent {
    override fun KtExpression.evaluate(): KaConstantValue? = withPsiValidityAssertion {
        val bindingContext = analysisContext.analyze(this)

        val constant = ConstantExpressionEvaluator.getPossiblyErrorConstant(this, bindingContext)

        // TODO: how to _not_ evaluate expressions with a compilation error, e.g., uninitialized property access
        if (constant?.usesNonConstValAsConstant == true) return null

        return constant?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.toKtConstantValueOrNull()
    }

    override fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue? = withPsiValidityAssertion {
        val bindingContext = analysisContext.analyze(this)
        val constant = ConstantExpressionEvaluator.getPossiblyErrorConstant(this, bindingContext)
        return constant?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.toKaAnnotationValue(analysisContext)
    }
}
