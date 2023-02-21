/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

abstract class IrDeclarationBuilder<Declaration : IrDeclaration> internal constructor() : IrElementBuilder<Declaration>(),
    IrAnnotationContainerBuilder {

    protected var declarationOrigin: IrDeclarationOrigin by SetAtMostOnce(IrDeclarationOrigin.DEFINED)

    fun origin(origin: IrDeclarationOrigin) {
        this.declarationOrigin = origin
    }

    override var builtAnnotations: List<IrConstructorCall> by SetAtMostOnce(emptyList())
}
