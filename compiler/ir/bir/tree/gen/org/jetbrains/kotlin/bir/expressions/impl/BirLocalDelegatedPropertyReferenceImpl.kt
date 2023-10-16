/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
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
    override var type: BirType,
    override var symbol: BirLocalDelegatedProperty,
    dispatchReceiver: BirExpression?,
    extensionReceiver: BirExpression?,
    override var origin: IrStatementOrigin?,
    override val typeArguments: Array<BirType?>,
    override var delegate: BirVariableSymbol,
    override var getter: BirSimpleFunctionSymbol,
    override var setter: BirSimpleFunctionSymbol?,
) : BirLocalDelegatedPropertyReference() {
    override var attributeOwnerId: BirAttributeContainer = this

    private var _dispatchReceiver: BirExpression? = dispatchReceiver

    override var dispatchReceiver: BirExpression?
        get() = _dispatchReceiver
        set(value) {
            if (_dispatchReceiver != value) {
                replaceChild(_dispatchReceiver, value)
                _dispatchReceiver = value
            }
        }

    private var _extensionReceiver: BirExpression? = extensionReceiver

    override var extensionReceiver: BirExpression?
        get() = _extensionReceiver
        set(value) {
            if (_extensionReceiver != value) {
                replaceChild(_extensionReceiver, value)
                _extensionReceiver = value
            }
        }

    override val valueArguments: BirChildElementList<BirExpression?> =
            BirChildElementList(this, 0)
    init {
        initChild(_dispatchReceiver)
        initChild(_extensionReceiver)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._dispatchReceiver === old -> this.dispatchReceiver = new as BirExpression
            this._extensionReceiver === old -> this.extensionReceiver = new as BirExpression
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when {
        id == 0 -> this.valueArguments
        else -> throwChildrenListWithIdNotFound(id)
    }
}
