/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.family.checkedexception

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectContext
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectContextCleaner
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectContextProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class CatchesExceptionCoeffectContextProvider(val exceptionType: ConeKotlinType, val marker: FirElement) : CoeffectContextProvider {
    override val family = CheckedExceptionCoeffectFamily

    override fun provideContext(context: CoeffectContext): CoeffectContext {
        if (context !is CheckedExceptionCoeffectContext) throw AssertionError()
        return CheckedExceptionCoeffectContext(context.catchesExceptions + (exceptionType to marker))
    }
}

class CheckedExceptionCoeffectContextCleaner(val exceptionType: ConeKotlinType, val marker: FirElement) : CoeffectContextCleaner {
    override val family = CheckedExceptionCoeffectFamily

    override fun cleanupContext(context: CoeffectContext): CoeffectContext {
        if (context !is CheckedExceptionCoeffectContext) throw AssertionError()
        return CheckedExceptionCoeffectContext(context.catchesExceptions - (exceptionType to marker))
    }
}