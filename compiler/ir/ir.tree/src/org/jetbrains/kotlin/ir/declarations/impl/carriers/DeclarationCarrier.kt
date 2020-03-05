/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.utils.SmartList

interface DeclarationCarrier<in T : DeclarationCarrier<T>>: Carrier<T> {
    var parentField: IrDeclarationParent?
    var originField: IrDeclarationOrigin
    var annotationsField: List<IrConstructorCall>
}