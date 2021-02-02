/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.isUnit
import org.jetbrains.kotlin.idea.quickfix.ChangeCallableReturnTypeFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.quickFixesHLApiBasedFactory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker


class ChangeTypeQuickFix internal constructor(
    declaration: KtCallableDeclaration,
    private val typeInfo: TypeInfo,
    private val updateBaseFunction: Boolean
) : KotlinQuickFixAction<KtCallableDeclaration>(declaration) {
    override fun getText(): String {
        val element = element ?: return ""
        val functionPresentation = when {
            updateBaseFunction -> {
                val containerName = element.parentOfType<KtNamedDeclaration>()?.nameAsName?.takeUnless { it.isSpecial }
                ChangeCallableReturnTypeFix.StringPresentation.baseFunctionOrConstructorParameterPresentation(element, containerName)
            }
            else -> null
        }
        return ChangeCallableReturnTypeFix.StringPresentation.getTextForQuickFix(
            element,
            functionPresentation,
            typeInfo.isUnit,
            typeInfo.short
        )
    }

    override fun getFamilyName(): String = ChangeCallableReturnTypeFix.StringPresentation.familyName()

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        if (!element.isProcedure()) {
            val newTypeRef = KtPsiFactory(project).createType(typeInfo.short)
            element.typeReference = newTypeRef
        } else {
            element.typeReference = null
        }
    }

    private fun KtCallableDeclaration.isProcedure() =
        typeInfo.isUnit && this is KtFunction && hasBlockBody()

    internal data class TypeInfo(
        val qualified: String,
        val short: String,
        val isUnit: Boolean,
    )

    companion object {
        val changeFunctionReturnTypeOnOverride =
            ChangeTypeQuickFixFactory.changeReturnTypeOnOverride<KtNamedDeclaration, KtFirDiagnostic.ReturnTypeMismatchOnOverride> {
                it.function as? KtFunctionSymbol
            }

        val changePropertyReturnTypeOnOverride =
            ChangeTypeQuickFixFactory.changeReturnTypeOnOverride<KtNamedDeclaration, KtFirDiagnostic.PropertyTypeMismatchOnOverride> {
                it.property as? KtPropertySymbol
            }

        val changeVariableReturnTypeOnOverride =
            ChangeTypeQuickFixFactory.changeReturnTypeOnOverride<KtNamedDeclaration, KtFirDiagnostic.VarTypeMismatchOnOverride> {
                it.variable as? KtPropertySymbol
            }
    }
}

private object ChangeTypeQuickFixFactory {
    inline fun <PSI : KtNamedDeclaration, DIAGNOSTIC : KtDiagnosticWithPsi<PSI>> changeReturnTypeOnOverride(
        crossinline getCallableSymbol: (DIAGNOSTIC) -> KtCallableSymbol?
    ) = quickFixesHLApiBasedFactory<PSI, DIAGNOSTIC> { diagnostic ->
        val declaration = diagnostic.psi as? KtCallableDeclaration ?: return@quickFixesHLApiBasedFactory emptyList()
        val callable = getCallableSymbol(diagnostic) ?: return@quickFixesHLApiBasedFactory emptyList()
        listOfNotNull(
            createChangeCurrentDeclarationQuickFix(callable, declaration),
            createChangeOverriddenFunctionQuickFix(callable),
        )
    }

    fun <PSI : KtCallableDeclaration> KtAnalysisSession.createChangeCurrentDeclarationQuickFix(
        callable: KtCallableSymbol,
        declaration: PSI
    ): ChangeTypeQuickFix? {
        val lowerSuperType = findLowerBoundOfOverriddenCallablesReturnTypes(callable) ?: return null
        val changeToTypeInfo = createTypeInfo(lowerSuperType)
        return ChangeTypeQuickFix(declaration, changeToTypeInfo, updateBaseFunction = false)
    }

    fun KtAnalysisSession.createChangeOverriddenFunctionQuickFix(
        callable: KtCallableSymbol
    ): ChangeTypeQuickFix? {
        val type = callable.annotatedType.type
        val singleNonMatchingOverriddenFunction = findSingleNonMatchingOverriddenFunction(callable, type) ?: return null
        val singleMatchingOverriddenFunctionPsi = singleNonMatchingOverriddenFunction.psiSafe<KtCallableDeclaration>() ?: return null
        val changeToTypeInfo = createTypeInfo(type)
        return ChangeTypeQuickFix(singleMatchingOverriddenFunctionPsi, changeToTypeInfo, updateBaseFunction = true)
    }

    private fun KtAnalysisSession.findSingleNonMatchingOverriddenFunction(
        callable: KtCallableSymbol,
        type: KtType
    ): KtCallableSymbol? {
        val overriddenSymbols = callable.getOverriddenSymbols()
        return overriddenSymbols
            .singleOrNull { overridden ->
                overridden.origin != KtSymbolOrigin.INTERSECTION_OVERRIDE && !type.isSubTypeOf(overridden.annotatedType.type)
            }
    }

    fun KtAnalysisSession.createTypeInfo(ktType: KtType) = ChangeTypeQuickFix.TypeInfo(
        qualified = ktType.render(),
        short = ktType.render(KtTypeRendererOptions.SHORT_NAMES),
        isUnit = ktType.isUnit
    )

    private fun KtAnalysisSession.findLowerBoundOfOverriddenCallablesReturnTypes(symbol: KtCallableSymbol): KtType? {
        var lowestType: KtType? = null
        for (overridden in symbol.getOverriddenSymbols()) {
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
