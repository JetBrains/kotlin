/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.mpp.TypeAliasSymbolMarker
import org.jetbrains.kotlin.name.StandardClassIds

object AbstractExpectActualAnnotationMatchChecker {
    class Incompatibility(val expectSymbol: DeclarationSymbolMarker, val actualSymbol: DeclarationSymbolMarker)

    fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        context: ExpectActualMatchingContext<*>,
    ): Incompatibility? = with(context) { areAnnotationsCompatible(expectSymbol, actualSymbol) }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        if (actualSymbol is TypeAliasSymbolMarker) {
            val expanded = actualSymbol.expandToRegularClass() ?: return null
            return areAnnotationsCompatible(expectSymbol, expanded)
        }

        val actualAnnotationsByName = actualSymbol.annotations.groupBy { it.classId }

        for (expectAnnotation in expectSymbol.annotations) {
            if (expectAnnotation.classId == StandardClassIds.Annotations.OptionalExpectation) {
                continue
            }
            val actualAnnotationsWithSameClassId = actualAnnotationsByName[expectAnnotation.classId] ?: emptyList()
            if (actualAnnotationsWithSameClassId.none { areAnnotationArgumentsEqual(expectAnnotation, it) }) {
                return Incompatibility(expectSymbol, actualSymbol)
            }
        }
        return null
    }
}