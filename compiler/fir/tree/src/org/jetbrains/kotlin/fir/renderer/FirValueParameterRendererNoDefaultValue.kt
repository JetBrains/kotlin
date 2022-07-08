/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirValueParameter

class FirValueParameterRendererNoDefaultValue : FirValueParameterRenderer() {
    override fun renderDefaultValue(valueParameter: FirValueParameter) {
        valueParameter.defaultValue?.let {
            printer.print(" = ...")
        }
    }
}
