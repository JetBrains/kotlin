/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.expressions.CoercionStrategy

internal class CallableReferenceAdaptation(
    val argumentTypes: Array<ConeKotlinType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: CallableReferenceMappedArguments<ConeResolutionAtom>,
    val suspendConversionStrategy: CallableReferenceConversionStrategy,
) {
    init {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            require(
                defaults != 0 ||
                        suspendConversionStrategy != CallableReferenceConversionStrategy.NoConversion ||
                        coercionStrategy != CoercionStrategy.NO_COERCION ||
                        mappedArguments.values.any { it is ResolvedCallArgument.VarargArgument }
            ) {
                "Adaptation must be non-trivial."
            }
        }
    }
}

sealed class CallableReferenceConversionStrategy {
    abstract val kind: FunctionTypeKind?

    object NoConversion : CallableReferenceConversionStrategy() {
        override val kind: FunctionTypeKind?
            get() = null
    }

    class CustomConversion(override val kind: FunctionTypeKind) : CallableReferenceConversionStrategy()
}
