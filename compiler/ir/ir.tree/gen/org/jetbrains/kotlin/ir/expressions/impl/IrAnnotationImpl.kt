/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrAnnotationArgsView
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name

@OptIn(DeprecatedCompilerApi::class)
class IrAnnotationImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    override var startOffset: Int,
    override var endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin?,
    override var source: SourceElement,
    override var constructorTypeArgumentsCount: Int,
    @property:DeprecatedCompilerApi(deprecatedSince = org.jetbrains.kotlin.CompilerVersionOfApiDeprecation._2_4_20)
    override var symbol: IrConstructorSymbol,
) : IrAnnotation() {
    override var attributeOwnerId: IrElement = this

    override val typeArguments: MutableList<IrType?> = ArrayList(0)

    override val classSymbol: IrClassSymbol
        get() = symbol.owner.parentAsClass.symbol

    override val argumentMapping: Map<Name, IrExpression?> = IrAnnotationArgsView(arguments, symbol)

    companion object
}
