/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.SpecialNames.NO_NAME_PROVIDED
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

sealed class FirExtensionShadowedByMemberChecker(kind: MppCheckerKind) : FirCallableDeclarationChecker(kind) {
    data object Regular : FirExtensionShadowedByMemberChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    data object ForExpectDeclaration : FirExtensionShadowedByMemberChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            declaration.hasAnnotation(StandardClassIds.Annotations.HidesMembers, context.session) ||
            declaration.receiverParameter?.typeRef?.coneType?.isNullable != false ||
            declaration.nameOrSpecialName == NO_NAME_PROVIDED ||
            // A common pattern, KT-70012
            declaration.isActual
        ) {
            return
        }

        val receiverSymbol = declaration.receiverParameter?.typeRef?.coneType
            ?.toClassLikeSymbol(context.session)
            ?.fullyExpandedClass(context.session)
            ?: return
        val scope = receiverSymbol.unsubstitutedScope(context)

        val shadowingMember = when (declaration) {
            is FirVariable -> findFirstSymbolByCondition<FirVariableSymbol<*>>(
                condition = { it.isVisible(context) && !it.isExtension },
                processMembers = { scope.processPropertiesByName(declaration.name, it) },
            )
            is FirSimpleFunction -> findFirstSymbolByCondition<FirNamedFunctionSymbol>(
                condition = { it.isVisible(context) && it.shadows(declaration.symbol, context) },
                processMembers = { scope.processFunctionsByName(declaration.name, it) },
            )
            else -> return
        }

        if (shadowingMember != null) {
            reporter.reportOn(declaration.source, FirErrors.EXTENSION_SHADOWED_BY_MEMBER, shadowingMember, context)
            return
        }

        if (declaration !is FirSimpleFunction) {
            return
        }

        val shadowingSymbols = findFirstNotNullSymbol(
            transform = { property ->
                if (!property.isVisible(context)) {
                    return@findFirstNotNullSymbol null
                }

                val returnTypeScope = property.resolvedReturnType.toClassLikeSymbol(context.session)
                    ?.fullyExpandedClass(context.session)
                    ?.unsubstitutedScope(context)
                    ?: return@findFirstNotNullSymbol null

                val invoke = findFirstSymbolByCondition(
                    condition = { it.isVisible(context) && it.isOperator && it.shadows(declaration.symbol, context) },
                    processMembers = { returnTypeScope.processFunctionsByName(OperatorNameConventions.INVOKE, it) },
                )

                invoke?.to(property)
            },
            processMembers = { scope.processPropertiesByName(declaration.name, it) },
        )

        val (shadowingInvoke, shadowingProperty) = shadowingSymbols ?: return

        reporter.reportOn(
            declaration.source,
            FirErrors.EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE,
            shadowingProperty, shadowingInvoke,
            context,
        )
    }

    private fun FirCallableSymbol<*>.isVisible(context: CheckerContext): Boolean {
        val useSiteFile = context.containingFile ?: error("No containing file present when running a checker for top-level functions")

        return context.session.visibilityChecker.isVisible(
            this, context.session, useSiteFile, context.containingDeclarations,
            dispatchReceiver = null,
        )
    }

    private inline fun <T> findFirstSymbolByCondition(
        crossinline condition: (T) -> Boolean,
        processMembers: ((T) -> Unit) -> Unit,
    ): T? = findFirstNotNullSymbol(
        transform = { it.takeIf { condition(it) } },
        processMembers = processMembers,
    )

    private inline fun <T, K> findFirstNotNullSymbol(
        crossinline transform: (T) -> K?,
        processMembers: ((T) -> Unit) -> Unit,
    ): K? {
        var found: K? = null

        processMembers { member ->
            if (found == null) {
                transform(member)?.let {
                    found = it
                }
            }
        }

        return found
    }

    /**
     * See [isExtensionFunctionShadowedByMemberFunction][org.jetbrains.kotlin.resolve.ShadowedExtensionChecker.isExtensionFunctionShadowedByMemberFunction]
     */
    private fun FirFunctionSymbol<*>.shadows(extension: FirFunctionSymbol<*>, context: CheckerContext): Boolean {
        if (isExtension) return false

        if (valueParameterSymbols.size != extension.valueParameterSymbols.size) return false
        if (varargParameterPosition != extension.varargParameterPosition) return false
        if (extension.isOperator && !isOperator) return false
        if (extension.isInfix && !isInfix) return false

        val helper = context.session.declarationOverloadabilityHelper
        val memberSignature = helper.createSignature(this)
        val extensionSignature = helper.createSignatureForPossiblyShadowedExtension(extension)

        return helper.isEquallyOrMoreSpecific(extensionSignature, memberSignature)
    }

    private val FirFunctionSymbol<*>.varargParameterPosition: Int
        get() = valueParameterSymbols.indexOfFirst { it.isVararg }
}
