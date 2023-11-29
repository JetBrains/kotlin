/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class BirTypeParameterImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    descriptor: TypeParameterDescriptor?,
    variance: Variance,
    index: Int,
    isReified: Boolean,
    superTypes: List<BirType>,
) : BirTypeParameter() {
    override val owner: BirTypeParameterImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead(8)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(8)
            }
        }

    private var _signature: IdSignature? = signature
    override var signature: IdSignature?
        get() {
            recordPropertyRead(9)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(9)
            }
        }

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead(2)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(2)
            }
        }

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead(3)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate(3)
            }
        }

    @ObsoleteDescriptorBasedAPI
    override val descriptor: TypeParameterDescriptor? = descriptor

    private var _variance: Variance = variance
    override var variance: Variance
        get() {
            recordPropertyRead(4)
            return _variance
        }
        set(value) {
            if (_variance != value) {
                _variance = value
                invalidate(4)
            }
        }

    private var _index: Int = index
    override var index: Int
        get() {
            recordPropertyRead(5)
            return _index
        }
        set(value) {
            if (_index != value) {
                _index = value
                invalidate(5)
            }
        }

    private var _isReified: Boolean = isReified
    override var isReified: Boolean
        get() {
            recordPropertyRead(6)
            return _isReified
        }
        set(value) {
            if (_isReified != value) {
                _isReified = value
                invalidate(6)
            }
        }

    private var _superTypes: List<BirType> = superTypes
    override var superTypes: List<BirType>
        get() {
            recordPropertyRead(7)
            return _superTypes
        }
        set(value) {
            if (_superTypes != value) {
                _superTypes = value
                invalidate(7)
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)

    init {
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.annotations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
