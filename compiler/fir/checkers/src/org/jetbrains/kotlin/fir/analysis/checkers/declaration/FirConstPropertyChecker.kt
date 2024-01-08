/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens

object FirConstPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isConst) return

        if (declaration.isVar) {
            val constModifier = declaration.getModifier(KtTokens.CONST_KEYWORD)
            constModifier?.let {
                reporter.reportOn(it.source, FirErrors.WRONG_MODIFIER_TARGET, it.token, "vars", context)
            }
        }

        val classKind = (context.containingDeclarations.lastOrNull() as? FirRegularClass)?.classKind
        if (classKind != ClassKind.OBJECT && context.containingDeclarations.size > 1) {
            reporter.reportOn(declaration.source, FirErrors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT, context)
            return
        }

        val source = declaration.getter?.source
        if (source != null && source.kind !is KtFakeSourceElementKind) {
            reporter.reportOn(source, FirErrors.CONST_VAL_WITH_GETTER, context)
            return
        }

        if (declaration.delegate != null) {
            reporter.reportOn(declaration.delegate?.source, FirErrors.CONST_VAL_WITH_DELEGATE, context)
            return
        }

        val initializer = declaration.initializer
        if (initializer == null) {
            reporter.reportOn(declaration.source, FirErrors.CONST_VAL_WITHOUT_INITIALIZER, context)
            return
        }

        val type = declaration.returnTypeRef.coneType.fullyExpandedType(context.session)
        if ((type !is ConeErrorType) && !type.canBeUsedForConstVal()) {
            reporter.reportOn(declaration.source, FirErrors.TYPE_CANT_BE_USED_FOR_CONST_VAL, declaration.returnTypeRef.coneType, context)
            return
        }

        val errorKind = when (checkConstantArguments(initializer, context.session)) {
            ConstantArgumentKind.VALID_CONST -> return
            ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION -> FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
            else -> FirErrors.CONST_VAL_WITH_NON_CONST_INITIALIZER
        }
        reporter.reportOn(initializer.source, errorKind, context)
    }
}
