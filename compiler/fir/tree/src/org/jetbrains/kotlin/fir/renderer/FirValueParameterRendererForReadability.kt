/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.arrayElementType

class FirValueParameterRendererForReadability : FirValueParameterRenderer() {
    override fun renderParameterType(valueParameter: FirValueParameter) {
        val returnTypeRef = valueParameter.returnTypeRef

        if (valueParameter.isVararg && returnTypeRef is FirResolvedTypeRef) {
            val arrayElementType = returnTypeRef.type.arrayElementType()
            if (arrayElementType != null) {
                typeRenderer.render(arrayElementType)
                return
            }
        }

        super.renderParameterType(valueParameter)
    }

    override fun renderDefaultValue(valueParameter: FirValueParameter) {
        valueParameter.defaultValue?.let {
            printer.print(" = ...")
        }
    }
}
