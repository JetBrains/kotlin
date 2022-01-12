/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol

abstract class IrVariable : IrValueDeclaration() {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: VariableDescriptor
    abstract override val symbol: IrVariableSymbol

    abstract val isVar: Boolean
    abstract val isConst: Boolean
    abstract val isLateinit: Boolean

    abstract var initializer: IrExpression?
}
