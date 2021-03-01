/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INITIALIZER_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.inference.isFunctionalType
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirInitializerTypeMismatchChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val initializer = declaration.initializer ?: return
        if (declaration.isDestructuringDeclaration) return
        if (initializer.isComponent) return
        val propertyType = declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val expressionType = initializer.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val typeContext = context.session.typeContext

        // hack: if property's type is (args) -> Unit, and lambda returns non-Unit value, the type of the lambda
        // will be non-unit, but it's OK
        if (propertyType.isFunctionalType(context.session)) {
            // getting property's expected return type
            val expectedType = propertyType.typeArguments.lastOrNull()
            if ((expectedType as? ConeClassLikeType)?.isUnit == true) {
                // dropping the return type (getting only the lambda args)
                val expectedArgs = propertyType.typeArguments.dropLast(1)
                val actualArgs = expressionType.typeArguments.dropLast(1)
                if (compareTypesList(actualArgs, expectedArgs, typeContext)) {
                    return
                }
            }
        }

        if (!AbstractTypeChecker.isSubtypeOf(typeContext, expressionType, propertyType)) {
            val source = declaration.source ?: return
            reporter.report(INITIALIZER_TYPE_MISMATCH.on(source, propertyType, expressionType), context)
        }
    }

    private val FirProperty.isDestructuringDeclaration
        get() = name.asString() == "<destruct>"
    private val FirExpression.isComponent
        get() = this is FirComponentCall

    private fun compareTypesList(
        expressionTypes: List<ConeTypeProjection>,
        propertyTypes: List<ConeTypeProjection>,
        context: ConeInferenceContext
    ): Boolean {
        if (expressionTypes.size != propertyTypes.size) return false

        for (i in expressionTypes.indices) {
            val expressionType = expressionTypes[i].type ?: return false
            val propertyType = propertyTypes[i].type ?: return false

            if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, expressionType, propertyType)) {
                return false
            }
        }

        return true
    }
}