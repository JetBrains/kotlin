/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.fixes.KtQuickFixRegistrar
import org.jetbrains.kotlin.idea.fir.api.fixes.KtQuickFixesList
import org.jetbrains.kotlin.idea.fir.api.fixes.KtQuickFixesListBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.quickfix.fixes.ChangeTypeQuickFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtParameter

class MainKtQuickFixRegistrar : KtQuickFixRegistrar() {
    private val modifiers = KtQuickFixesListBuilder.registerPsiQuickFix {
        registerPsiQuickFix<PsiElement, KtFirDiagnostic.RedundantModifier>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = true))
        registerPsiQuickFix<PsiElement, KtFirDiagnostic.IncompatibleModifiers>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = false))
        registerPsiQuickFix<PsiElement, KtFirDiagnostic.RepeatedModifier>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = false))
        registerPsiQuickFix<PsiElement, KtFirDiagnostic.DeprecatedModifierPair>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = true))
        registerPsiQuickFix<PsiElement, KtFirDiagnostic.TypeParametersInEnum>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = true))
        registerPsiQuickFix<KtModifierListOwner, KtFirDiagnostic.RedundantOpenInInterface>(
            RemoveModifierFix.createRemoveModifierFromListOwnerFactoryByModifierListOwner(
                modifier = KtTokens.OPEN_KEYWORD,
                isRedundant = true
            )
        )
    }

    private val overrides = KtQuickFixesListBuilder.registerPsiQuickFix {
        registerApplicator(ChangeTypeQuickFix.changeFunctionReturnTypeOnOverride)
        registerApplicator(ChangeTypeQuickFix.changePropertyReturnTypeOnOverride)
        registerApplicator(ChangeTypeQuickFix.changeVariableReturnTypeOnOverride)
    }

    private val mutability = KtQuickFixesListBuilder.registerPsiQuickFix {
        registerPsiQuickFix<PsiElement, KtFirDiagnostic.VarOverriddenByVal>(ChangeVariableMutabilityFix.VAR_OVERRIDDEN_BY_VAL_FACTORY)
        registerPsiQuickFix<KtParameter, KtFirDiagnostic.VarAnnotationParameter>(ChangeVariableMutabilityFix.VAR_ANNOTATION_PARAMETER_FACTORY)
        registerPsiQuickFix<KtModifierListOwner, KtFirDiagnostic.InapplicableLateinitModifier>(ChangeVariableMutabilityFix.LATEINIT_VAL_FACTORY)
    }

    override val list: KtQuickFixesList = KtQuickFixesList.createCombined(
        modifiers,
        overrides,
        mutability,
    )
}