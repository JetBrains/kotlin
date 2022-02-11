/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer

interface Fir2IrTypeParametersContainer : IrTypeParametersContainer, Fir2IrComponents {
    val fir: FirMemberDeclaration

    fun prepareTypeParameters() {
        typeParameters = fir.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            classifierStorage.getIrTypeParameter(typeParameter, index, symbol).apply {
                parent = this@Fir2IrTypeParametersContainer
                if (superTypes.isEmpty()) {
                    superTypes = typeParameter.bounds.map { it.toIrType(typeConverter) }
                }
            }
        }
    }
}
