/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.fir.api.*
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.fir.api.applicator.applicator
import org.jetbrains.kotlin.idea.fir.api.fixes.HLApplicatorTargetWithInput
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.fir.api.fixes.withInput
import org.jetbrains.kotlin.idea.fir.applicators.CallableReturnTypeUpdaterApplicator
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.quickfix.ChangeCallableReturnTypeFix
import org.jetbrains.kotlin.psi.*

object ChangeTypeQuickFix {
    val applicator = applicator<KtCallableDeclaration, Input> {
        familyName(CallableReturnTypeUpdaterApplicator.applicator.getFamilyName())

        actionName { declaration, (updateBaseFunction, type) ->
            val presentation = getPresentation(updateBaseFunction, declaration)
            getActionName(declaration, presentation, type)
        }

        applyTo { declaration, (_, type), project, editor ->
            CallableReturnTypeUpdaterApplicator.applicator.applyTo(declaration, type, project, editor)
        }
    }

    private fun getActionName(
        declaration: KtCallableDeclaration,
        presentation: String?,
        type: CallableReturnTypeUpdaterApplicator.Type
    ) = ChangeCallableReturnTypeFix.StringPresentation.getTextForQuickFix(
        declaration,
        presentation,
        type.isUnit,
        type.shortTypeRepresentation
    )

    private fun getPresentation(
        updateBaseFunction: Boolean,
        declaration: KtCallableDeclaration
    ) = when {
        updateBaseFunction -> {
            val containerName = declaration.parentOfType<KtNamedDeclaration>()?.nameAsName?.takeUnless { it.isSpecial }
            ChangeCallableReturnTypeFix.StringPresentation.baseFunctionOrConstructorParameterPresentation(
                declaration,
                containerName
            )
        }
        else -> null
    }

    data class Input(
        val updateBaseFunction: Boolean,
        val type: CallableReturnTypeUpdaterApplicator.Type
    ) : HLApplicatorInput {
        override fun isValidFor(psi: PsiElement): Boolean = type.isValidFor(psi)
    }

    val changeFunctionReturnTypeOnOverride =
        changeReturnTypeOnOverride<KtFirDiagnostic.ReturnTypeMismatchOnOverride> {
            it.function as? KtFunctionSymbol
        }

    val changePropertyReturnTypeOnOverride =
        changeReturnTypeOnOverride<KtFirDiagnostic.PropertyTypeMismatchOnOverride> {
            it.property as? KtPropertySymbol
        }

    val changeVariableReturnTypeOnOverride =
        changeReturnTypeOnOverride<KtFirDiagnostic.VarTypeMismatchOnOverride> {
            it.variable as? KtPropertySymbol
        }


    private inline fun <DIAGNOSTIC : KtDiagnosticWithPsi<KtNamedDeclaration>> changeReturnTypeOnOverride(
        crossinline getCallableSymbol: (DIAGNOSTIC) -> KtCallableSymbol?
    ) = diagnosticFixFactory<DIAGNOSTIC, KtCallableDeclaration, Input>(applicator) { diagnostic ->
        val declaration = diagnostic.psi as? KtCallableDeclaration ?: return@diagnosticFixFactory emptyList()
        val callable = getCallableSymbol(diagnostic) ?: return@diagnosticFixFactory emptyList()
        listOfNotNull(
            createChangeCurrentDeclarationQuickFix(callable, declaration),
            createChangeOverriddenFunctionQuickFix(callable),
        )
    }

    private fun <PSI : KtCallableDeclaration> KtAnalysisSession.createChangeCurrentDeclarationQuickFix(
        callable: KtCallableSymbol,
        declaration: PSI
    ): HLApplicatorTargetWithInput<PSI, Input>? {
        val lowerSuperType = findLowerBoundOfOverriddenCallablesReturnTypes(callable) ?: return null
        val changeToTypeInfo = createTypeInfo(lowerSuperType)
        return declaration withInput Input(updateBaseFunction = false, changeToTypeInfo)
    }

    private fun KtAnalysisSession.createChangeOverriddenFunctionQuickFix(
        callable: KtCallableSymbol
    ): HLApplicatorTargetWithInput<KtCallableDeclaration, Input>? {
        val type = callable.annotatedType.type
        val singleNonMatchingOverriddenFunction = findSingleNonMatchingOverriddenFunction(callable, type) ?: return null
        val singleMatchingOverriddenFunctionPsi = singleNonMatchingOverriddenFunction.psiSafe<KtCallableDeclaration>() ?: return null
        val changeToTypeInfo = createTypeInfo(type)
        return singleMatchingOverriddenFunctionPsi withInput Input(updateBaseFunction = true, changeToTypeInfo)
    }

    private fun KtAnalysisSession.findSingleNonMatchingOverriddenFunction(
        callable: KtCallableSymbol,
        type: KtType
    ): KtCallableSymbol? {
        val overriddenSymbols = callable.getDirectlyOverriddenSymbols()
        return overriddenSymbols
            .singleOrNull { overridden ->
                !type.isSubTypeOf(overridden.annotatedType.type)
            }
    }

    private fun KtAnalysisSession.createTypeInfo(ktType: KtType) = with(CallableReturnTypeUpdaterApplicator.Type) {
        createByKtType(ktType)
    }

    private fun KtAnalysisSession.findLowerBoundOfOverriddenCallablesReturnTypes(symbol: KtCallableSymbol): KtType? {
        var lowestType: KtType? = null
        for (overridden in symbol.getDirectlyOverriddenSymbols()) {
            val overriddenType = overridden.annotatedType.type
            when {
                lowestType == null || overriddenType isSubTypeOf lowestType -> {
                    lowestType = overriddenType
                }
                lowestType isNotSubTypeOf overriddenType -> {
                    return null
                }
            }
        }
        return lowestType
    }
}
