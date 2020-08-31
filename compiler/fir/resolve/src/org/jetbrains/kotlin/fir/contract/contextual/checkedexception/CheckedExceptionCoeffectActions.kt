/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.checkedexception

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContext
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContextCleaner
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContextProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class CatchesExceptionCoeffectContextProvider(val exceptionType: ConeKotlinType, val marker: FirElement) : CoeffectContextProvider {
    override val family = CheckedExceptionCoeffectFamily

    override fun provideContext(context: CoeffectContext): CoeffectContext {
        if (context !is CheckedExceptionCoeffectContext) throw AssertionError()
        val checkedExceptions = context.checkedExceptions.toMutableMap()
        checkedExceptions[marker] = exceptionType
        return CheckedExceptionCoeffectContext(checkedExceptions)
    }
}

class CheckedExceptionCoeffectContextCleaner(val marker: FirElement) : CoeffectContextCleaner {
    override val family = CheckedExceptionCoeffectFamily

    override fun cleanupContext(context: CoeffectContext): CoeffectContext {
        if (context !is CheckedExceptionCoeffectContext) throw AssertionError()
        val checkedExceptions = context.checkedExceptions.toMutableMap()
        checkedExceptions.remove(marker)
        return CheckedExceptionCoeffectContext(checkedExceptions)
    }
}