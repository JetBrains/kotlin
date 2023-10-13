/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirLocalDelegatedProperty
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirLocalDelegatedPropertyReference
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.symbols.BirVariableSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirLocalDelegatedPropertyReferenceImpl(
    override var sourceSpan: SourceSpan,
    override var originalBeforeInline: BirAttributeContainer?,
    override var type: BirType,
    override var symbol: BirLocalDelegatedProperty,
    override var dispatchReceiver: BirExpression?,
    override var extensionReceiver: BirExpression?,
    override var origin: IrStatementOrigin?,
    override val valueArguments: Array<BirExpression?>,
    override val typeArguments: Array<BirType?>,
    override var delegate: BirVariableSymbol,
    override var getter: BirSimpleFunctionSymbol,
    override var setter: BirSimpleFunctionSymbol?,
) : BirLocalDelegatedPropertyReference() {
    override var attributeOwnerId: BirAttributeContainer = this
}
