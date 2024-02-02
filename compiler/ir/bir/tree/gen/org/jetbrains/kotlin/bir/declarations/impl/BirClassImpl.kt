/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirClassImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
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
) : BirImplElementBase(), BirClass, BirClassSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        kind: ClassKind,
        modality: Modality,
        source: SourceElement,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        isExternal = false,
        visibility = visibility,
        kind = kind,
        modality = modality,
        isCompanion = false,
        isInner = false,
        isData = false,
        isValue = false,
        isExpect = false,
        isFun = false,
        hasEnumEntries = false,
        source = source,
        superTypes = emptyList(),
        thisReceiver = null,
        valueClassRepresentation = null,
    )

    override val owner: BirClassImpl
        get() = this

    override val isBound: Boolean
        get() = true

    private var _sourceSpan: SourceSpan = sourceSpan
    override var sourceSpan: SourceSpan
        get() {
            recordPropertyRead()
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    override val signature: IdSignature? = signature

    private var _annotations: List<BirConstructorCall> = annotations
    override var annotations: List<BirConstructorCall>
        get() {
            recordPropertyRead()
            return _annotations
        }
        set(value) {
            if (_annotations != value) {
                _annotations = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead()
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead()
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    private var _isExternal: Boolean = isExternal
    override var isExternal: Boolean
        get() {
            recordPropertyRead()
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate()
            }
        }

    private var _visibility: DescriptorVisibility = visibility
    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead()
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate()
            }
        }

    private var _attributeOwnerId: BirAttributeContainer = this
    override var attributeOwnerId: BirAttributeContainer
        get() {
            recordPropertyRead()
            return _attributeOwnerId
        }
        set(value) {
            if (_attributeOwnerId !== value) {
                _attributeOwnerId = value
                invalidate()
            }
        }

    override val symbol: BirClassSymbol
        get() = this

    private var _kind: ClassKind = kind
    override var kind: ClassKind
        get() {
            recordPropertyRead()
            return _kind
        }
        set(value) {
            if (_kind != value) {
                _kind = value
                invalidate()
            }
        }

    private var _modality: Modality = modality
    override var modality: Modality
        get() {
            recordPropertyRead()
            return _modality
        }
        set(value) {
            if (_modality != value) {
                _modality = value
                invalidate()
            }
        }

    private var _isCompanion: Boolean = isCompanion
    override var isCompanion: Boolean
        get() {
            recordPropertyRead()
            return _isCompanion
        }
        set(value) {
            if (_isCompanion != value) {
                _isCompanion = value
                invalidate()
            }
        }

    private var _isInner: Boolean = isInner
    override var isInner: Boolean
        get() {
            recordPropertyRead()
            return _isInner
        }
        set(value) {
            if (_isInner != value) {
                _isInner = value
                invalidate()
            }
        }

    private var _isData: Boolean = isData
    override var isData: Boolean
        get() {
            recordPropertyRead()
            return _isData
        }
        set(value) {
            if (_isData != value) {
                _isData = value
                invalidate()
            }
        }

    private var _isValue: Boolean = isValue
    override var isValue: Boolean
        get() {
            recordPropertyRead()
            return _isValue
        }
        set(value) {
            if (_isValue != value) {
                _isValue = value
                invalidate()
            }
        }

    private var _isExpect: Boolean = isExpect
    override var isExpect: Boolean
        get() {
            recordPropertyRead()
            return _isExpect
        }
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate()
            }
        }

    private var _isFun: Boolean = isFun
    override var isFun: Boolean
        get() {
            recordPropertyRead()
            return _isFun
        }
        set(value) {
            if (_isFun != value) {
                _isFun = value
                invalidate()
            }
        }

    private var _hasEnumEntries: Boolean = hasEnumEntries
    override var hasEnumEntries: Boolean
        get() {
            recordPropertyRead()
            return _hasEnumEntries
        }
        set(value) {
            if (_hasEnumEntries != value) {
                _hasEnumEntries = value
                invalidate()
            }
        }

    override val source: SourceElement = source

    private var _superTypes: List<BirType> = superTypes
    override var superTypes: List<BirType>
        get() {
            recordPropertyRead()
            return _superTypes
        }
        set(value) {
            if (_superTypes != value) {
                _superTypes = value
                invalidate()
            }
        }

    private var _thisReceiver: BirValueParameter? = thisReceiver
    override var thisReceiver: BirValueParameter?
        get() {
            recordPropertyRead()
            return _thisReceiver
        }
        set(value) {
            if (_thisReceiver !== value) {
                childReplaced(_thisReceiver, value)
                _thisReceiver = value
                invalidate()
            }
        }

    private var _valueClassRepresentation: ValueClassRepresentation<BirSimpleType>? = valueClassRepresentation
    override var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?
        get() {
            recordPropertyRead()
            return _valueClassRepresentation
        }
        set(value) {
            if (_valueClassRepresentation != value) {
                _valueClassRepresentation = value
                invalidate()
            }
        }

    override val typeParameters: BirImplChildElementList<BirTypeParameter> = BirImplChildElementList(this, 1, false)

    override val declarations: BirImplChildElementList<BirDeclaration> = BirImplChildElementList(this, 2, false)


    init {
        initChild(_thisReceiver)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        typeParameters.acceptChildrenLite(visitor)
        declarations.acceptChildrenLite(visitor)
        _thisReceiver?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._thisReceiver === old -> {
                this._thisReceiver = new as BirValueParameter?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.typeParameters
            2 -> this.declarations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
