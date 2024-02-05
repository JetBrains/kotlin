/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.name.StandardClassIds

object FirDataSealedArgumentClassChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.hasAnnotation(StandardClassIds.Annotations.DataArgument, context.session)) {
            if (!checkDataArgument(declaration)) {
                reporter.reportOn(declaration.source, FirErrors.INCORRECT_DATAARG_CLASS)
            }
        }
        if (declaration.hasAnnotation(StandardClassIds.Annotations.SealedArgument, context.session)) {
            if (!checkSealedArgument(declaration)) {
                reporter.reportOn(declaration.source, FirErrors.INCORRECT_SEALEDARG_CLASS)
            }
        }
    }

    context(context: CheckerContext)
    private fun checkDataArgument(declaration: FirClass): Boolean {
        val constructor = declaration.primaryConstructorIfAny(context.session) ?: return false
        for (parameter in constructor.valueParameterSymbols) {
            if (parameter.isVararg) return false
            if (parameter.isDataArgument) return false
            if (parameter.isSealedArgument) return false
            if (!parameter.hasDefaultValue) return false
        }
        return true
    }

    context(context: CheckerContext)
    private fun checkSealedArgument(declaration: FirClass): Boolean  {
        if (declaration !is FirRegularClass) return false
        if (!declaration.isSealed) return false

        val knownTypes = mutableSetOf<ConeKotlinType>()
        for (subclassId in declaration.getSealedClassInheritors(context.session)) {
            val subclass = context.session.getRegularClassSymbolByClassId(subclassId) ?: return false
            val constructor = subclass.primaryConstructorIfAny(context.session) ?: return false
            if (constructor.typeParameterSymbols.isNotEmpty()) return false
            if (constructor.valueParameterSymbols.size != 1) return false
            val singleParameter = constructor.valueParameterSymbols.single()
            if (singleParameter.isVararg) return false
            if (singleParameter.isDataArgument) return false
            if (singleParameter.isSealedArgument) return false
            val type = singleParameter.resolvedReturnType
            if (knownTypes.any { type.isSubtypeOf(it, context.session) || it.isSubtypeOf(type, context.session) }) return false
            knownTypes.add(type)
        }
        return true
    }
}