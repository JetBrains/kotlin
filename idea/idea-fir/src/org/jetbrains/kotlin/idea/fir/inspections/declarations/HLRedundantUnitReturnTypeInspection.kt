/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.inspections.declarations

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.fir.api.*
import org.jetbrains.kotlin.idea.fir.api.applicator.inputProvider
import org.jetbrains.kotlin.idea.fir.api.applicator.presentation
import org.jetbrains.kotlin.idea.fir.api.applicator.with
import org.jetbrains.kotlin.idea.fir.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.idea.fir.applicators.CallableReturnTypeUpdaterApplicator
import org.jetbrains.kotlin.idea.frontend.api.types.isUnit
import org.jetbrains.kotlin.psi.KtNamedFunction

internal class HLRedundantUnitReturnTypeInspection :
    AbstractHLInspection<KtNamedFunction, CallableReturnTypeUpdaterApplicator.Type>(
        KtNamedFunction::class
    ), CleanupLocalInspectionTool {

    override val applicabilityRange = ApplicabilityRanges.CALLABLE_RETURN_TYPE

    override val applicator = CallableReturnTypeUpdaterApplicator.applicator.with {
        isApplicableByPsi { callable ->
            val function = callable as? KtNamedFunction ?: return@isApplicableByPsi false
            function.hasBlockBody() && function.typeReference != null
        }
        familyName(KotlinBundle.lazyMessage("remove.explicit.type.specification"))
        actionName(KotlinBundle.lazyMessage("redundant.unit.return.type"))
    }

    override val inputProvider = inputProvider<KtNamedFunction, CallableReturnTypeUpdaterApplicator.Type> { function ->
        when {
            function.getFunctionLikeSymbol().annotatedType.type.isUnit -> CallableReturnTypeUpdaterApplicator.Type.UNIT
            else -> null
        }
    }

    override val presentation = presentation<KtNamedFunction> {
        highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
    }
}
