/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.backend.common.actualizer.reportActualAnnotationConflictingDefaultArgumentValue
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy

internal object IrExpectActualAnnotationConflictingDefaultArgumentValueChecker : IrExpectActualChecker {
    override fun check(context: IrExpectActualChecker.Context) = with(context) {
        for ((expectSymbol, actualSymbol) in matchedExpectToActual) {
            if (expectSymbol !is IrConstructorSymbol || actualSymbol !is IrConstructorSymbol) continue

            val expectClass = expectSymbol.owner.parentAsClass
            if (expectClass.kind != ClassKind.ANNOTATION_CLASS) continue

            val expectValueParams = expectSymbol.owner.valueParameters
            val actualValueParams = actualSymbol.owner.valueParameters
            if (expectValueParams.size != actualValueParams.size) continue

            for ((expectParam, actualParam) in expectValueParams.zip(actualValueParams)) {
                val expectDefaultValue = expectParam.defaultValue?.expression ?: continue
                val actualDefaultValue = actualParam.defaultValue?.expression ?: continue
                with(matchingContext) {
                    if (!areIrExpressionConstValuesEqual(
                            expectDefaultValue, actualDefaultValue,
                            ExpectActualCollectionArgumentsCompatibilityCheckStrategy.Default
                        )
                    ) {
                        // TODO(Roman.Efremov): KT-62104 fix failing tests and unmute
                        // reportError(expectClass, actualDefaultValue, actualParam)
                    }
                }
            }
        }
    }

    @Suppress("unused")
    private fun IrExpectActualChecker.Context.reportError(
        expectAnnotationClass: IrClass,
        actualDefaultValue: IrExpression,
        actualParam: IrValueParameter,
    ) {
        val actualTypealias = getTypealiasSymbolIfActualizedViaTypealias(expectAnnotationClass, classActualizationInfo)?.owner
        if (actualTypealias != null) {
            diagnosticsReporter.reportActualAnnotationConflictingDefaultArgumentValue(
                actualTypealias, actualTypealias.file, actualParam
            )
            return
        }

        diagnosticsReporter.reportActualAnnotationConflictingDefaultArgumentValue(
            actualDefaultValue, actualParam.file, actualParam
        )
    }
}