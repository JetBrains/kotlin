/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import org.jetbrains.kotlin.idea.fir.api.fixes.KtQuickFixRegistrar
import org.jetbrains.kotlin.idea.fir.api.fixes.KtQuickFixesList
import org.jetbrains.kotlin.idea.fir.api.fixes.KtQuickFixesListBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.quickfix.fixes.ChangeTypeQuickFix

class MainKtQuickFixRegistrar : KtQuickFixRegistrar() {
    private val modifiers = KtQuickFixesListBuilder.registerPsiQuickFix {
        registerPsiQuickFixes(KtFirDiagnostic.RedundantModifier::class, RemoveModifierFix.removeRedundantModifier)
        registerPsiQuickFixes(KtFirDiagnostic.IncompatibleModifiers::class, RemoveModifierFix.removeNonRedundantModifier)
        registerPsiQuickFixes(KtFirDiagnostic.RepeatedModifier::class, RemoveModifierFix.removeNonRedundantModifier)
        registerPsiQuickFixes(KtFirDiagnostic.DeprecatedModifierPair::class, RemoveModifierFix.removeRedundantModifier)
        registerPsiQuickFixes(KtFirDiagnostic.TypeParametersInEnum::class, RemoveModifierFix.removeRedundantModifier)
        registerPsiQuickFixes(KtFirDiagnostic.RedundantOpenInInterface::class, RemoveModifierFix.removeRedundantOpenModifier)
        registerPsiQuickFixes(KtFirDiagnostic.NonAbstractFunctionWithNoBody::class, AddFunctionBodyFix, AddModifierFix.addAbstractModifier)
        registerPsiQuickFixes(
            KtFirDiagnostic.AbstractPropertyInNonAbstractClass::class,
            AddModifierFix.addAbstractToContainingClass,
            RemoveModifierFix.removeAbstractModifier
        )
        registerPsiQuickFixes(
            KtFirDiagnostic.AbstractFunctionInNonAbstractClass::class,
            AddModifierFix.addAbstractToContainingClass,
            RemoveModifierFix.removeAbstractModifier
        )
        registerPsiQuickFixes(
            KtFirDiagnostic.NonFinalMemberInFinalClass::class,
            AddModifierFix.addOpenToContainingClass,
            RemoveModifierFix.removeOpenModifier
        )
        registerPsiQuickFixes(
            KtFirDiagnostic.PrivateSetterForOpenProperty::class,
            AddModifierFix.addFinalToProperty,
            RemoveModifierFix.removePrivateModifier
        )
        registerPsiQuickFixes(
            KtFirDiagnostic.PrivateSetterForAbstractProperty::class,
            RemoveModifierFix.removePrivateModifier
        )
        registerPsiQuickFixes(
            KtFirDiagnostic.NestedClassNotAllowed::class,
            AddModifierFix.addInnerModifier
        )
    }

    private val overrides = KtQuickFixesListBuilder.registerPsiQuickFix {
        registerApplicator(ChangeTypeQuickFix.changeFunctionReturnTypeOnOverride)
        registerApplicator(ChangeTypeQuickFix.changePropertyReturnTypeOnOverride)
        registerApplicator(ChangeTypeQuickFix.changeVariableReturnTypeOnOverride)
    }

    private val mutability = KtQuickFixesListBuilder.registerPsiQuickFix {
        registerPsiQuickFixes(KtFirDiagnostic.VarOverriddenByVal::class, ChangeVariableMutabilityFix.VAR_OVERRIDDEN_BY_VAL_FACTORY)
        registerPsiQuickFixes(KtFirDiagnostic.VarAnnotationParameter::class, ChangeVariableMutabilityFix.VAR_ANNOTATION_PARAMETER_FACTORY)
        registerPsiQuickFixes(KtFirDiagnostic.InapplicableLateinitModifier::class, ChangeVariableMutabilityFix.LATEINIT_VAL_FACTORY)
        registerPsiQuickFixes(KtFirDiagnostic.ValWithSetter::class, ChangeVariableMutabilityFix.VAL_WITH_SETTER_FACTORY)
    }

    override val list: KtQuickFixesList = KtQuickFixesList.createCombined(
        modifiers,
        overrides,
        mutability,
    )
}
