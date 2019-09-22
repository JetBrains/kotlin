/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

interface IrClassifier : IrDeclaration, IrSymbolOwner, TypeConstructorMarker {
    override val symbol: IrClassifierSymbol
    override val descriptor: ClassifierDescriptor
}