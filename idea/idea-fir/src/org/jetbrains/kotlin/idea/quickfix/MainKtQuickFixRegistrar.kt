/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.quickfix.fixes.ChangeTypeQuickFix
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner

class MainKtQuickFixRegistrar : KtQuickFixRegistrar() {
    private val modifiers = KtQuickFixesListBuilder.register {
        register<PsiElement, KtFirDiagnostic.RedundantModifier>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = true))
        register<PsiElement, KtFirDiagnostic.IncompatibleModifiers>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = false))
        register<PsiElement, KtFirDiagnostic.RepeatedModifier>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = false))
        register<PsiElement, KtFirDiagnostic.DeprecatedModifierPair>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = true))
        register<PsiElement, KtFirDiagnostic.TypeParametersInEnum>(RemoveModifierFix.createRemoveModifierFactory(isRedundant = true))
        register<KtModifierListOwner, KtFirDiagnostic.RedundantOpenInInterface>(
            RemoveModifierFix.createRemoveModifierFromListOwnerFactoryByModifierListOwner(
                modifier = KtTokens.OPEN_KEYWORD,
                isRedundant = true
            )
        )
    }

    override val list: KtQuickFixesList = KtQuickFixesList.createCombined(
        modifiers,
    )
}