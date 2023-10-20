/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.expressions.FirGetClassCall

internal class FirGetClassCallRendererForDebugging : FirGetClassCallRenderer() {
    override fun render(getClassCall: FirGetClassCall) {
        components.annotationRenderer?.render(getClassCall)
        components.printer.print("<getClass>")
        components.visitor.visitCall(getClassCall)
    }
}
