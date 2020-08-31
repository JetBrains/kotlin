/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.checkedexception

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContext
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContextCombiner
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectFamily
import org.jetbrains.kotlin.fir.contracts.contextual.declaration.coeffectActionExtractors
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object CheckedExceptionCoeffectFamily : CoeffectFamily {
    override val id = "Checked Exceptions"
    override val emptyContext = CheckedExceptionCoeffectContext(mapOf())
    override val combiner = CheckedExceptionCoeffectContextCombiner
}

data class CheckedExceptionCoeffectContext(val checkedExceptions: Map<FirElement, ConeKotlinType>) : CoeffectContext

object CheckedExceptionCoeffectContextCombiner : CoeffectContextCombiner {
    override fun merge(left: CoeffectContext, right: CoeffectContext): CoeffectContext {
        if (left !is CheckedExceptionCoeffectContext || right !is CheckedExceptionCoeffectContext) throw AssertionError()
        val exceptions = left.checkedExceptions.filter { right.checkedExceptions.containsKey(it.key) }
        return CheckedExceptionCoeffectContext(exceptions)
    }
}

fun throwsEffectCoeffectExtractors(exceptionType: ConeKotlinType) = coeffectActionExtractors {
    family = CheckedExceptionCoeffectFamily

    onOwnerCall {
        actions {
            verifiers += CheckedExceptionCoeffectContextVerifier(exceptionType)
        }
    }

    onOwnerEnter {
        actions {
            providers += CatchesExceptionCoeffectContextProvider(exceptionType, it)
        }
    }
}

fun calledInTryCatchEffectCoeffectExtractors(exceptionType: ConeKotlinType) = coeffectActionExtractors {
    family = CheckedExceptionCoeffectFamily

    onOwnerCall {
        actions {
            verifiers += CheckedExceptionCoeffectContextVerifier(exceptionType)
        }
    }

    onOwnerEnter {
        actions {
            providers += CatchesExceptionCoeffectContextProvider(exceptionType, it)
        }
    }
}