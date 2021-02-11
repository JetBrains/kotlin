/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.inspections.diagnosticBased

import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinBundleIndependent
import org.jetbrains.kotlin.idea.fir.api.AbstractHLDiagnosticBasedInspection
import org.jetbrains.kotlin.idea.fir.api.applicator.*
import org.jetbrains.kotlin.idea.fir.api.inputByDiagnosticProvider
import org.jetbrains.kotlin.idea.fir.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.idea.fir.applicators.ModifierApplicators
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

class HLRedundantVisibilityModifierInspection :
    AbstractHLDiagnosticBasedInspection<KtModifierListOwner, KtFirDiagnostic.RedundantVisibilityModifier, ModifierApplicators.Modifier>(
        elementType = KtModifierListOwner::class,
        diagnosticType = KtFirDiagnostic.RedundantVisibilityModifier::class
    ) {

    override val inputByDiagnosticProvider =
        inputByDiagnosticProvider<KtModifierListOwner, KtFirDiagnostic.RedundantVisibilityModifier, ModifierApplicators.Modifier> { diagnostic ->
            val modifier = diagnostic.psi.visibilityModifierType() ?: return@inputByDiagnosticProvider null
            ModifierApplicators.Modifier(modifier)
        }

    override val presentation: HLPresentation<KtModifierListOwner> = presentation {
        highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
    }

    override val applicabilityRange: HLApplicabilityRange<KtModifierListOwner> = ApplicabilityRanges.VISIBILITY_MODIFIER

    override val applicator: HLApplicator<KtModifierListOwner, ModifierApplicators.Modifier> =
        ModifierApplicators.removeModifierApplicator(
            KtTokens.VISIBILITY_MODIFIERS,
            KotlinBundle.lazyMessage("redundant.visibility.modifier")
        ).with {
            actionName { _, (modifier) -> KotlinBundleIndependent.message("remove.redundant.0.modifier", modifier.value) }
        }
}