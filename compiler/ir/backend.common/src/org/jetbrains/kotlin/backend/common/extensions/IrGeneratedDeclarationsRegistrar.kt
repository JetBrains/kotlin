/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

abstract class IrGeneratedDeclarationsRegistrar {
    abstract fun addMetadataVisibleAnnotationsToElement(declaration: IrDeclaration, annotations: List<IrConstructorCall>)

    fun addMetadataVisibleAnnotationsToElement(declaration: IrDeclaration, vararg annotations: IrConstructorCall) {
        addMetadataVisibleAnnotationsToElement(declaration, annotations.toList())
    }

    abstract fun registerFunctionAsMetadataVisible(irFunction: IrSimpleFunction)
    abstract fun registerConstructorAsMetadataVisible(irConstructor: IrConstructor)

    // TODO: KT-63881
    // abstract fun registerPropertyAsMetadataVisible(irProperty: IrProperty)
}
