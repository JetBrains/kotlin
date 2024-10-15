/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.arrayElementType

class FirCallableSignatureRendererForReadability : FirCallableSignatureRenderer() {
    override fun renderCallableType(callableDeclaration: FirCallableDeclaration) {
        val returnTypeRef = callableDeclaration.returnTypeRef

        if (callableDeclaration is FirValueParameter && callableDeclaration.isVararg && returnTypeRef is FirResolvedTypeRef) {
            val arrayElementType = returnTypeRef.coneType.arrayElementType()
            if (arrayElementType != null) {
                typeRenderer.render(arrayElementType)
                return
            }
        }

        super.renderCallableType(callableDeclaration)
    }

    override fun renderDefaultValue(valueParameter: FirValueParameter) {
        valueParameter.defaultValue?.let {
            printer.print(" = ...")
        }
    }
}
