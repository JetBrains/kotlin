/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall

class FirResolvePhaseRenderer {
    internal lateinit var components: FirRendererComponents
    private val printer get() = components.printer

    fun render(element: FirElementWithResolveState) {
        @OptIn(ResolveStateAccess::class)
        val text = when (element) {
            is FirSyntheticProperty -> "[<synthetic> ${element.resolvePhase}] "
            is FirSyntheticPropertyAccessor -> "[<synthetic> ${element.delegate.resolveState}] "
            else -> "[${element.resolveState}] "
        }

        printer.print(text)
    }

    fun render(element: FirAnnotationCall) {
        printer.print("[${element.annotationResolvePhase}]")
    }
}
