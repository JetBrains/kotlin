/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirClassImpl(
    sourceSpan: CompressedSourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    kind: ClassKind,
    modality: Modality,
    isCompanion: Boolean,
    isInner: Boolean,
    isData: Boolean,
    isValue: Boolean,
    isExpect: Boolean,
    isFun: Boolean,
    hasEnumEntries: Boolean,
    source: SourceElement,
    superTypes: List<BirType>,
    thisReceiver: BirValueParameter?,
    valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?,
) : BirImplElementBase(BirClass), BirClass {
    override val owner: BirClassImpl
        get() = this

    private var _sourceSpan: CompressedSourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan
        get() {
            recordPropertyRead(14)
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate(14)
            }
        }

    private var _signature: IdSignature? = signature
    override var signature: IdSignature?
        get() {
            recordPropertyRead(14)
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate(14)
            }
        }

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead(6)
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate(6)
            }
        }

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead(7)
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate(7)
            }
        }

    private var _isExternal: Boolean = isExternal
    override var isExternal: Boolean
        get() {
            recordPropertyRead(8)
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate(8)
            }
        }

    private var _visibility: DescriptorVisibility = visibility
    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead(9)
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate(9)
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this
    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead(5)
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId !== value) {
                _attributeOwnerId = value
                invalidate(5)
            }
        }

    private var _kind: ClassKind = kind
    override var kind: ClassKind
        get() {
            recordPropertyRead(10)
            return _kind
        }
        set(value) {
            if (_kind != value) {
                _kind = value
                invalidate(10)
            }
        }

    private var _modality: Modality = modality
    override var modality: Modality
        get() {
            recordPropertyRead(11)
            return _modality
        }
        set(value) {
            if (_modality != value) {
                _modality = value
                invalidate(11)
            }
        }

    private var _isCompanion: Boolean = isCompanion
    override var isCompanion: Boolean
        get() {
            recordPropertyRead(12)
            return _isCompanion
        }
        set(value) {
            if (_isCompanion != value) {
                _isCompanion = value
                invalidate(12)
            }
        }

    private var _isInner: Boolean = isInner
    override var isInner: Boolean
        get() {
            recordPropertyRead(13)
            return _isInner
        }
        set(value) {
            if (_isInner != value) {
                _isInner = value
                invalidate(13)
            }
        }

    private var _isData: Boolean = isData
    override var isData: Boolean
        get() {
            recordPropertyRead(14)
            return _isData
        }
        set(value) {
            if (_isData != value) {
                _isData = value
                invalidate(14)
            }
        }

    private var _isValue: Boolean = isValue
    override var isValue: Boolean
        get() {
            recordPropertyRead(14)
            return _isValue
        }
        set(value) {
            if (_isValue != value) {
                _isValue = value
                invalidate(14)
            }
        }

    private var _isExpect: Boolean = isExpect
    override var isExpect: Boolean
        get() {
            recordPropertyRead(14)
            return _isExpect
        }
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate(14)
            }
        }

    private var _isFun: Boolean = isFun
    override var isFun: Boolean
        get() {
            recordPropertyRead(14)
            return _isFun
        }
        set(value) {
            if (_isFun != value) {
                _isFun = value
                invalidate(14)
            }
        }

    private var _hasEnumEntries: Boolean = hasEnumEntries
    /**
     * Returns true iff this is a class loaded from dependencies which has the `HAS_ENUM_ENTRIES` metadata flag set.
     * This flag is useful for Kotlin/JVM to determine whether an enum class from dependency actually has the `entries` property
     * in its bytecode, as opposed to whether it has it in its member scope, which is true even for enum classes compiled by
     * old versions of Kotlin which did not support the EnumEntries language feature.
     */
    override var hasEnumEntries: Boolean
        get() {
            recordPropertyRead(14)
            return _hasEnumEntries
        }
        set(value) {
            if (_hasEnumEntries != value) {
                _hasEnumEntries = value
                invalidate(14)
            }
        }

    override val source: SourceElement = source

    private var _superTypes: List<BirType> = superTypes
    override var superTypes: List<BirType>
        get() {
            recordPropertyRead(14)
            return _superTypes
        }
        set(value) {
            if (_superTypes != value) {
                _superTypes = value
                invalidate(14)
            }
        }

    private var _thisReceiver: BirValueParameter? = thisReceiver
    override var thisReceiver: BirValueParameter?
        get() {
            recordPropertyRead(4)
            return _thisReceiver
        }
        set(value) {
            if (_thisReceiver !== value) {
                childReplaced(_thisReceiver, value)
                _thisReceiver = value
                invalidate(4)
            }
        }

    private var _valueClassRepresentation: ValueClassRepresentation<BirSimpleType>? = valueClassRepresentation
    override var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?
        get() {
            recordPropertyRead(14)
            return _valueClassRepresentation
        }
        set(value) {
            if (_valueClassRepresentation != value) {
                _valueClassRepresentation = value
                invalidate(14)
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)
    override val typeParameters: BirImplChildElementList<BirTypeParameter> = BirImplChildElementList(this, 2, false)
    override val declarations: BirImplChildElementList<BirDeclaration> = BirImplChildElementList(this, 3, false)

    init {
        initChild(_thisReceiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        typeParameters.acceptChildrenLite(visitor)
        declarations.acceptChildrenLite(visitor)
        _thisReceiver?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        return when {
            this._thisReceiver === old -> {
                this._thisReceiver = new as BirValueParameter?
                4
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.annotations
            2 -> this.typeParameters
            3 -> this.declarations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
