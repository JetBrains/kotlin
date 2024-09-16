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
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.StandardClassIds

object FirIterableCollectionOverloadsInFileChecker : FirFileChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        _check(declaration.declarations, context, reporter)
    }
}

object FirIterableOverloadsInClassChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        _check(declaration.declarations, context, reporter)
    }
}

private val iterable = ConeClassLikeTypeImpl(
    StandardClassIds.Iterable.toLookupTag(),
    arrayOf(
        ConeClassLikeTypeImpl(
            StandardClassIds.Any.toLookupTag(),
            ConeTypeProjection.EMPTY_ARRAY,
            isMarkedNullable = false
        )
    ),
    isMarkedNullable = false
)

private val collection = ConeClassLikeTypeImpl(
    StandardClassIds.Collection.toLookupTag(),
    arrayOf(
        ConeClassLikeTypeImpl(
            StandardClassIds.Any.toLookupTag(),
            ConeTypeProjection.EMPTY_ARRAY,
            isMarkedNullable = false
        )
    ),
    isMarkedNullable = false
)

private fun _check(declarations: List<FirDeclaration>, context: CheckerContext, reporter: DiagnosticReporter) {
    for (overloadGroup in declarations.filterIsInstance<FirSimpleFunction>().groupBy { it.name }.values) {
        var paramIndex = 0
        while (true) {
            val params = overloadGroup.mapNotNull { fn -> fn.valueParameters.getOrNull(paramIndex)?.let { fn to it } }
                .takeIf { it.isNotEmpty() }
                ?: break

            val iterableParams = params.filter { (_, param) -> param.returnTypeRef.coneType.isSubtypeOf(iterable, context.session) }
            if (iterableParams.size > 1) {
                reporter.reportOn(
                    iterableParams.first().second.source,
                    FirErrors.BOBKO_ITERABLE_OVERLOADS,
                    iterableParams.first().first.render(),
                    context
                )
            } else {
                val collectionParams = params.filter { (_, param) -> param.returnTypeRef.coneType.isSubtypeOf(collection, context.session) }
                if (collectionParams.size > 1) {
                    reporter.reportOn(
                        collectionParams.first().second.source,
                        FirErrors.BOBKO_COLLECTION_OVERLOADS,
                        iterableParams.first().first.render(),
                        context
                    )
                }
            }

            paramIndex++
        }
    }
}
