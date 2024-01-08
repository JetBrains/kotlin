/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterInQualifiedAccess
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

// See FE1.0 [KClassWithIncorrectTypeArgumentChecker]
object FirKClassWithIncorrectTypeArgumentChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        // Only report on top level callable declarations
        if (context.containingDeclarations.size > 1) return

        // When a type parameter is used as a type argument for KClass, it shouldn't be nullable.
        //  bad:  fun <T> test1() = T::class
        //  okay: fun <T: Any> test2() = T::class
        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val typeArgumentsWithWrongType = mutableListOf<ConeKotlinType>()

        val returnType = declaration.returnTypeRef.coneType
        if (returnType.isKClassTypeWithErrorOrNullableArgument(context.session.typeContext)) typeArgumentsWithWrongType.add(returnType)

        returnType.typeArguments.forEach {
            val type = it.type ?: return@forEach
            if (type.isKClassTypeWithErrorOrNullableArgument(context.session.typeContext))
                typeArgumentsWithWrongType.add(type)
        }

        if (typeArgumentsWithWrongType.isEmpty()) return
        typeArgumentsWithWrongType.forEach {
            val typeParameterFromError = (it.typeArguments[0] as? ConeKotlinTypeProjection)?.type?.typeParameterFromError ?: return@forEach
            reporter.reportOn(source, FirErrors.KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE, typeParameterFromError, context)
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
            argumentType.typeParameterFromError?.let { typeParameterSymbol ->
                return typeParameterSymbol.toConeType().isNullableType()
            }
            return argumentType is ConeErrorType || argumentType.isNullableType()
        }
    }

    private val ConeKotlinType.typeParameterFromError: FirTypeParameterSymbol?
        get() = ((this as? ConeErrorType)?.diagnostic as? ConeTypeParameterInQualifiedAccess)?.symbol

}
