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
import org.jetbrains.kotlin.fir.declarations.utils.isInfix
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.SpecialNames.NO_NAME_PROVIDED
import org.jetbrains.kotlin.name.StandardClassIds

object FirExtensionShadowedByMemberChecker : FirCallableDeclarationChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            declaration.hasAnnotation(StandardClassIds.Annotations.HidesMembers, context.session) ||
            declaration.receiverParameter?.typeRef?.coneType?.isNullable != false ||
            declaration.nameOrSpecialName == NO_NAME_PROVIDED
        ) {
            return
        }

        val receiverSymbol = declaration.receiverParameter?.typeRef?.coneType
            ?.toClassLikeSymbol(context.session)
            ?.fullyExpandedClass(context.session)
            ?: return
        val scope = receiverSymbol.unsubstitutedScope(context)

        val shadowingSymbol = when (declaration) {
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

        if (shadowingSymbol != null) {
            reporter.reportOn(declaration.source, FirErrors.EXTENSION_SHADOWED_BY_MEMBER, shadowingSymbol, context)
        }
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
    ): T? {
        var found: T? = null

        processMembers { member ->
            if (found == null && condition(member)) {
                found = member
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
