/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.types.*

object FirAmbiguousAnonymousTypeChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirFunction && declaration !is FirProperty) return
        require(declaration is FirCallableDeclaration)
        // if source is not null then this type was declared in source
        // so it can not be inferred to anonymous type
        if (declaration.symbol.hasExplicitReturnType) return
        if (context.containingDeclarations.any { it.isLocalMember || it is FirAnonymousObject }) return

        if (!shouldApproximateAnonymousTypesOfNonLocalDeclaration(
                declaration.visibilityForApproximation(context.containingDeclarations.lastOrNull()),
                declaration.isInline
            )
        ) return

        /*
         * Here we want to check only cases when type of declaration was inferred from single-expression block
         * There are three possible cases for it:
         * 1. `fun foo() = ...`
         * 2. `val x = ...`
         * 3. `val x get() = ...`
         */
        val type = when (declaration) {
            is FirProperty -> declaration.initializer?.resolvedType ?: declaration.getter?.body?.singleExpressionType
            is FirFunction -> declaration.body?.singleExpressionType
            else -> error("Should not be there")
        } ?: return

        checkTypeAndArguments(type, context, reporter, declaration.source)
    }

    private fun checkTypeAndArguments(
        type: ConeKotlinType,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        reportOn: KtSourceElement?
    ) {
        val classSymbol = type.toSymbol(context.session)
        if (classSymbol is FirAnonymousObjectSymbol && classSymbol.resolvedSuperTypeRefs.size > 1) {
            // Any anonymous object that has only one super type is already approximated to the super type by
            // org.jetbrains.kotlin.fir.types.TypeUtilsKt#hideLocalTypeIfNeeded. Hence, any remaining anonymous object must have more than
            // one super types and hence are ambiguous.
            reporter.reportOn(
                reportOn,
                FirErrors.AMBIGUOUS_ANONYMOUS_TYPE_INFERRED,
                classSymbol.resolvedSuperTypeRefs.map { it.coneType },
                context
            )
        }
        for (typeArgument in type.typeArguments) {
            checkTypeAndArguments(
                typeArgument.type ?: continue,
                context, reporter, reportOn
            )
        }
    }

    private val FirBlock.singleExpressionType
        get() = ((this as? FirSingleExpressionBlock)?.statement as? FirReturnExpression)?.result?.resolvedType
}
