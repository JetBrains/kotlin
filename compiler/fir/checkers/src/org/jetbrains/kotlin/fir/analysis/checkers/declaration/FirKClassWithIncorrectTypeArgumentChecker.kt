/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterInQualifiedAccess
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.isKClassType
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.*

// See FE1.0 [KClassWithIncorrectTypeArgumentChecker]
object FirKClassWithIncorrectTypeArgumentChecker : FirFileChecker() {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        for (topLevelDeclaration in declaration.declarations) {
            if (topLevelDeclaration is FirCallableDeclaration) {
                checkTopLevelDeclaration(topLevelDeclaration, context, reporter)
            }
        }
    }

    // When a type parameter is used as a type argument for KClass, it shouldn't be nullable.
    //  bad:  fun <T> test1() = T::class
    //  okay: fun <T: Any> test2() = T::class
    private fun checkTopLevelDeclaration(
        declaration: FirCallableDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val source = declaration.source ?: return
        if (source.kind is FirFakeSourceElementKind) return

        val returnType = declaration.returnTypeRef.coneType
        if (!returnType.isKClassTypeWithErrorOrNullableArgument(context.session.inferenceComponents.ctx)) return

        val typeArgument = (returnType.typeArguments[0] as ConeKotlinTypeProjection).type
        typeArgument.typeParameterFromError?.let {
            reporter.reportOn(source, FirErrors.KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE, it.symbol, context)
        }
    }

    private fun ConeKotlinType.isKClassTypeWithErrorOrNullableArgument(context: ConeInferenceContext): Boolean {
        if (!this.isKClassType()) return false
        val argumentType = typeArguments.toList().singleOrNull()?.let {
            when (it) {
                is ConeStarProjection -> null
                is ConeKotlinTypeProjection -> it.type
            }
        } ?: return false
        with(context) {
            argumentType.typeParameterFromError?.let { typeParameter ->
                return typeParameter.toConeType().isNullableType()
            }
            return argumentType is ConeKotlinErrorType || argumentType.isNullableType()
        }
    }

    private val ConeKotlinType.typeParameterFromError: FirTypeParameter?
        get() = ((this as? ConeKotlinErrorType)?.diagnostic as? ConeTypeParameterInQualifiedAccess)?.symbol?.fir

}
