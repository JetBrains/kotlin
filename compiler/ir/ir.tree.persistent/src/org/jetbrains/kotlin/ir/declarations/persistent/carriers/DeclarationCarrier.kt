/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface DeclarationCarrier : Carrier {
    val parentSymbolField: IrSymbol?
        get() = parentField?.cast<IrSymbolOwner>()?.symbol

    val parentField: IrDeclarationParent?
        get() = parentSymbolField?.owner?.cast()

    val originField: IrDeclarationOrigin
    val annotationsField: List<IrConstructorCall>
}
