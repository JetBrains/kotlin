/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.family.checkedexception

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectContext
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectContextVerifier
import org.jetbrains.kotlin.fir.contract.contextual.diagnostics.CoeffectContextVerificationError
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.AbstractTypeChecker

class CheckedExceptionContextError(val exceptionType: ConeKotlinType) : CoeffectContextVerificationError

class CheckedExceptionCoeffectContextVerifier(val exceptionType: ConeKotlinType) : CoeffectContextVerifier {
    override val family = CheckedExceptionCoeffectFamily

    override fun verifyContext(context: CoeffectContext, session: FirSession): List<CoeffectContextVerificationError> {
        if (context !is CheckedExceptionCoeffectContext) throw AssertionError()
        val checked = context.catchesExceptions.any { AbstractTypeChecker.isSubtypeOf(session.typeContext, exceptionType, it.first) }
        return if (!checked) {
            listOf(CheckedExceptionContextError(exceptionType))
        } else emptyList()
    }
}