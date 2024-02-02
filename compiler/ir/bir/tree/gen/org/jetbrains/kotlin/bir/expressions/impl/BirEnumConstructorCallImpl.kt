/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirEnumConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.symbols.BirConstructorSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.ForwardReferenceRecorder
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

class BirEnumConstructorCallImpl(
    sourceSpan: CompressedSourceSpan,
    type: BirType,
    dispatchReceiver: BirExpression?,
    extensionReceiver: BirExpression?,
    origin: IrStatementOrigin?,
    typeArguments: List<BirType?>,
    contextReceiversCount: Int,
    symbol: BirConstructorSymbol,
) : BirEnumConstructorCall(BirEnumConstructorCall) {
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    private var _dispatchReceiver: BirExpression? = dispatchReceiver
    override var dispatchReceiver: BirExpression?
        get() {
            return _dispatchReceiver
        }
        set(value) {
            if (_dispatchReceiver !== value) {
                childReplaced(_dispatchReceiver, value)
                _dispatchReceiver = value
            }
        }

    private var _extensionReceiver: BirExpression? = extensionReceiver
    override var extensionReceiver: BirExpression?
        get() {
            return _extensionReceiver
        }
        set(value) {
            if (_extensionReceiver !== value) {
                childReplaced(_extensionReceiver, value)
                _extensionReceiver = value
            }
        }

    override var origin: IrStatementOrigin? = origin

    override var typeArguments: List<BirType?> = typeArguments

    override var contextReceiversCount: Int = contextReceiversCount

    override var symbol: BirConstructorSymbol = symbol
        set(value) {
            if (field != value) {
                field = value
                forwardReferencePropertyChanged()
            }
        }

    override val valueArguments: BirImplChildElementList<BirExpression?> = BirImplChildElementList(this, 1, true)

    init {
        initChild(_dispatchReceiver)
        initChild(_extensionReceiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _dispatchReceiver?.acceptLite(visitor)
        _extensionReceiver?.acceptLite(visitor)
        valueArguments.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._dispatchReceiver === old -> {
                this._dispatchReceiver = new as BirExpression?
            }
            this._extensionReceiver === old -> {
                this._extensionReceiver = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.valueArguments
            else -> throwChildrenListWithIdNotFound(id)
        }
    }

    override fun getForwardReferences(recorder: ForwardReferenceRecorder) {
        recorder.recordReference(symbol)
    }
}
