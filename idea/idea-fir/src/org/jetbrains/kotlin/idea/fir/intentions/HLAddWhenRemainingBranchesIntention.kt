/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.intentions

import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.idea.fir.api.AbstractHLIntention
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicabilityRange
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInputProvider
import org.jetbrains.kotlin.idea.fir.api.applicator.inputProvider
import org.jetbrains.kotlin.idea.fir.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.idea.quickfix.fixes.AddWhenRemainingBranchFixFactories
import org.jetbrains.kotlin.psi.KtWhenExpression

class HLAddWhenRemainingBranchesIntention : AbstractHLIntention<KtWhenExpression, AddWhenRemainingBranchFixFactories.Input>(
    KtWhenExpression::class,
    AddWhenRemainingBranchFixFactories.applicator
) {
    override val applicabilityRange: HLApplicabilityRange<KtWhenExpression> get() = ApplicabilityRanges.SELF

    override val inputProvider: HLApplicatorInputProvider<KtWhenExpression, AddWhenRemainingBranchFixFactories.Input>
        get() = inputProvider { element ->
            // TODO: consider removing the condition below so that this intention also works if there is no else. Currently we only offer
            //  this intention if there is an `else` branch so that it behaves identically with FE1.0 (because FE1.0 reports warnings for
            //  non-exhaustive when, which then results in a quickfix).
            if (element.entries.none { it.isElse }) return@inputProvider null
            val whenMissingCases = element.getMissingCases().takeIf {
                it.isNotEmpty() && it.singleOrNull() != WhenMissingCase.Unknown
            } ?: return@inputProvider null
            AddWhenRemainingBranchFixFactories.Input(whenMissingCases, null)
        }
}