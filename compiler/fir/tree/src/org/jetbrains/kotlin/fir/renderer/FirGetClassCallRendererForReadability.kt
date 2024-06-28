/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.expressions.FirGetClassCall

class FirGetClassCallRendererForReadability : FirGetClassCallRenderer() {
    override fun render(getClassCall: FirGetClassCall) {
        getClassCall.argument.accept(components.visitor)
        components.printer.print("::class")
    }
}