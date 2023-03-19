/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection

class ConeTypeRendererForDebugging() : ConeTypeRenderer() {

    constructor(builder: StringBuilder) : this() {
        this.builder = builder
        this.idRenderer = ConeIdRendererForDebugging()
        idRenderer.builder = builder
    }

    override fun renderAsPossibleFunctionType(
        type: ConeKotlinType,
        functionClassKindExtractor: (ConeKotlinType) -> FunctionTypeKind?,
        renderType: ConeTypeProjection.() -> Unit
    ) {
        builder.append("R|")
        super.renderAsPossibleFunctionType(type, functionClassKindExtractor, renderType)
        builder.append("|")
    }
}
