/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.findChildByType
import org.jetbrains.kotlin.diagnostics.findChildrenByType
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirContextParametersDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source?.kind is KtFakeSourceElementKind) return

        val contextListSources = declaration.source?.findContextReceiverListSources().orEmpty().ifEmpty { return }

        val source = contextListSources.first()

        if (contextListSources.size > 1) {
            reporter.reportOn(source, FirErrors.MULTIPLE_CONTEXT_LISTS, context)
        }

        val contextReceiversEnabled = context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)
        val contextParametersEnabled = context.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)

        val errorMessage = when (declaration) {
            // Stuff that was never supported
            is FirTypeAlias -> "Context parameters on type aliases are unsupported."
            is FirAnonymousInitializer -> "Context parameters on initializers are unsupported."
            is FirEnumEntry -> "Context parameters on enum entries are unsupported."
            is FirPropertyAccessor -> "Context parameters on property accessors are unsupported."
            is FirBackingField -> "Context parameters on backing fields are unsupported."
            is FirPrimaryConstructor -> "Context parameters on primary constructors are unsupported."
            is FirProperty if declaration.isLocal -> "Context parameters on local properties are unsupported.".takeIf { contextParametersEnabled }
            // Stuff that is unsupported with context parameters
            is FirConstructor -> "Context parameters on constructors are unsupported.".takeIf { contextParametersEnabled }
            is FirClass -> "Context parameters on classes are unsupported.".takeIf { contextParametersEnabled }
            is FirCallableDeclaration if declaration.isDelegationOperator() -> "Context parameters on delegation operators are unsupported.".takeIf { contextParametersEnabled }
            is FirProperty if declaration.delegate != null -> "Context parameters on delegated properties are unsupported.".takeIf { contextParametersEnabled }
            // Only valid positions
            is FirSimpleFunction, is FirProperty, is FirAnonymousFunction -> null
            // Fallback if we forgot something.
            else -> "Context parameters are unsupported in this position."
        }

        if (errorMessage != null) {
            reporter.reportOn(
                source,
                FirErrors.UNSUPPORTED,
                errorMessage,
                context
            )
        }

        val contextParameters = declaration.getContextParameters()
        if (contextParameters.isEmpty()) return

        if (!contextReceiversEnabled && !contextParametersEnabled) {
            reporter.reportOn(
                source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.ContextParameters to context.languageVersionSettings,
                context
            )
            return
        }

        if (contextReceiversEnabled) {
            if (checkSubTypes(contextParameters.map { it.returnTypeRef.coneType }, context)) {
                reporter.reportOn(
                    source,
                    FirErrors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS,
                    context
                )
            }
            for (parameter in contextParameters) {
                if (!parameter.isLegacyContextReceiver()) {
                    reporter.reportOn(
                        parameter.source,
                        FirErrors.UNSUPPORTED_FEATURE,
                        LanguageFeature.ContextParameters to context.languageVersionSettings,
                        context
                    )
                }
            }
        }

        if (contextParametersEnabled) {
            for (parameter in contextParameters) {
                if (parameter.isLegacyContextReceiver()) {
                    reporter.reportOn(parameter.source, FirErrors.CONTEXT_PARAMETER_WITHOUT_NAME, context)
                }

                parameter.source?.getModifierList()?.modifiers?.forEach { modifier ->
                    reporter.reportOn(modifier.source, FirErrors.WRONG_MODIFIER_TARGET, modifier.token, "context parameter", context)
                }

                FirFunctionParameterChecker.checkValOrVar(parameter, reporter, context)
            }
        }
    }

    private fun FirCallableDeclaration.isDelegationOperator(): Boolean {
        return this.isOperator && this.nameOrSpecialName in OperatorNameConventions.DELEGATED_PROPERTY_OPERATORS
    }

    private fun FirDeclaration.getContextParameters(): List<FirValueParameter> {
        return when (this) {
            is FirCallableDeclaration -> contextParameters
            is FirRegularClass -> contextParameters
            else -> emptyList()
        }
    }

    private fun KtSourceElement.findContextReceiverListSources(): List<KtSourceElement> {
        return when (this) {
            is KtPsiSourceElement ->
                psi.getChildOfType<KtModifierList>()?.contextReceiverLists?.map { it.toKtPsiSourceElement() }.orEmpty()
            is KtLightSourceElement ->
                treeStructure.findChildByType(lighterASTNode, KtNodeTypes.MODIFIER_LIST)
                    ?.let { treeStructure.findChildrenByType(it, KtNodeTypes.CONTEXT_RECEIVER_LIST) }
                    ?.map { it.toKtLightSourceElement(treeStructure) }
                    .orEmpty()
        }
    }

    /**
     * Simplified checking of subtype relation used in context receiver checkers.
     * It converts type parameters to star projections and top level type parameters to its supertypes. Then it checks the relation.
     */
    fun checkSubTypes(types: List<ConeKotlinType>, context: CheckerContext): Boolean {
        fun replaceTypeParametersByStarProjections(type: ConeClassLikeType): ConeClassLikeType {
            return type.withArguments(type.typeArguments.map {
                when {
                    it.isStarProjection -> it
                    it.type!! is ConeTypeParameterType -> ConeStarProjection
                    it.type!! is ConeClassLikeType -> replaceTypeParametersByStarProjections(it.type as ConeClassLikeType)
                    else -> it
                }
            }.toTypedArray())
        }

        val replacedTypeParameters = types.flatMap { r ->
            when (r) {
                is ConeTypeParameterType -> r.lookupTag.typeParameterSymbol.resolvedBounds.map { it.coneType }
                is ConeClassLikeType -> listOf(replaceTypeParametersByStarProjections(r))
                else -> listOf(r)
            }
        }

        for (i in replacedTypeParameters.indices)
            for (j in i + 1..<replacedTypeParameters.size) {
                if (replacedTypeParameters[i].isSubtypeOf(replacedTypeParameters[j], context.session)
                    || replacedTypeParameters[j].isSubtypeOf(replacedTypeParameters[i], context.session)
                )
                    return true
            }

        return false
    }
}

