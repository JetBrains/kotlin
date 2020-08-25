/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.declaration

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectContextActions
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectFamily
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall

fun interface CoeffectActionExtractor<F : FirElement> {
    fun extractActions(fir: F): CoeffectContextActions
}

fun interface OwnerCallCoeffectActionExtractor : CoeffectActionExtractor<FirFunctionCall>
fun interface OwnerEnterCoeffectActionExtractor : CoeffectActionExtractor<FirFunction<*>>
fun interface OwnerExitCoeffectActionExtractor : CoeffectActionExtractor<FirFunction<*>>

class CoeffectActionExtractors(
    val family: CoeffectFamily,
    val onOwnerCall: OwnerCallCoeffectActionExtractor? = null,
    val onOwnerEnter: OwnerEnterCoeffectActionExtractor? = null,
    val onOwnerExit: OwnerExitCoeffectActionExtractor? = null
)

class CoeffectActionExtractorsBuilder {

    var family: CoeffectFamily? = null

    private var onOwnerCall: OwnerCallCoeffectActionExtractor? = null
    private var onOwnerEnter: OwnerEnterCoeffectActionExtractor? = null
    private var onOwnerExit: OwnerExitCoeffectActionExtractor? = null

    fun noActions(): CoeffectContextActions = CoeffectContextActions.EMPTY

    fun onOwnerCall(extractor: OwnerCallCoeffectActionExtractor) {
        onOwnerCall = extractor
    }

    fun onOwnerEnter(extractor: OwnerEnterCoeffectActionExtractor) {
        onOwnerEnter = extractor
    }

    fun onOwnerExit(extractor: OwnerExitCoeffectActionExtractor) {
        onOwnerExit = extractor
    }

    fun build(): CoeffectActionExtractors {
        val family = family ?: throw AssertionError("Undefined coeffect family for extractors")
        return CoeffectActionExtractors(family, onOwnerCall, onOwnerEnter, onOwnerExit)
    }

}


