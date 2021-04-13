/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isComponentCall
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeForTypeMismatch
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INITIALIZER_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

object FirInitializerTypeMismatchChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val initializer = declaration.initializer ?: return
        val source = declaration.source ?: return
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) return
        if (initializer.isComponentCall) return
        if (declaration.returnTypeRef.source?.kind != FirRealSourceElementKind) return
        val propertyType = declaration.returnTypeRef.coneType
        val expressionType = initializer.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val typeContext = context.session.typeContext

        if (!isSubtypeForTypeMismatch(typeContext, subtype = expressionType, supertype = propertyType)) {
            if (expressionType is ConeClassLikeType &&
                expressionType.lookupTag.classId == StandardClassIds.Int &&
                propertyType.fullyExpandedType(context.session).isIntegerTypeOrNullableIntegerTypeOfAnySize &&
                expressionType.nullability == ConeNullability.NOT_NULL
            ) {
                // val p: Byte = 42 or similar situation
                // TODO: remove after fix of KT-46047
                return
            }
            if (propertyType.isExtensionFunctionType || expressionType.isExtensionFunctionType) {
                // TODO: remove after fix of KT-45989
                return
            }
            reporter.report(INITIALIZER_TYPE_MISMATCH.on(source, propertyType, expressionType), context)
        }
    }
}