/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirClassImpl @ObsoleteDescriptorBasedAPI constructor(
    sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor,
    signature: IdSignature?,
    override var annotations: List<BirConstructorCall>,
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
    override val source: SourceElement,
    override var superTypes: List<BirType>,
    thisReceiver: BirValueParameter?,
    valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?,
) : BirClass() {
    override val owner: BirClassImpl
        get() = this

    private var _sourceSpan: SourceSpan = sourceSpan

    override var sourceSpan: SourceSpan
        get() = _sourceSpan
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature? = signature

    override var signature: IdSignature?
        get() = _signature
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin

    override var origin: IrDeclarationOrigin
        get() = _origin
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _name: Name = name

    override var name: Name
        get() = _name
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    private var _isExternal: Boolean = isExternal

    override var isExternal: Boolean
        get() = _isExternal
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate()
            }
        }

    private var _visibility: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() = _visibility
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate()
            }
        }

    override var typeParameters: BirChildElementList<BirTypeParameter> =
            BirChildElementList(this, 0)

    override val declarations: BirChildElementList<BirDeclaration> =
            BirChildElementList(this, 1)

    private var _attributeOwnerId: BirAttributeContainer = this

    override var attributeOwnerId: BirAttributeContainer
        get() = _attributeOwnerId
        set(value) {
            if (_attributeOwnerId != value) {
                _attributeOwnerId = value
                invalidate()
            }
        }

    private var _kind: ClassKind = kind

    override var kind: ClassKind
        get() = _kind
        set(value) {
            if (_kind != value) {
                _kind = value
                invalidate()
            }
        }

    private var _modality: Modality = modality

    override var modality: Modality
        get() = _modality
        set(value) {
            if (_modality != value) {
                _modality = value
                invalidate()
            }
        }

    private var _isCompanion: Boolean = isCompanion

    override var isCompanion: Boolean
        get() = _isCompanion
        set(value) {
            if (_isCompanion != value) {
                _isCompanion = value
                invalidate()
            }
        }

    private var _isInner: Boolean = isInner

    override var isInner: Boolean
        get() = _isInner
        set(value) {
            if (_isInner != value) {
                _isInner = value
                invalidate()
            }
        }

    private var _isData: Boolean = isData

    override var isData: Boolean
        get() = _isData
        set(value) {
            if (_isData != value) {
                _isData = value
                invalidate()
            }
        }

    private var _isValue: Boolean = isValue

    override var isValue: Boolean
        get() = _isValue
        set(value) {
            if (_isValue != value) {
                _isValue = value
                invalidate()
            }
        }

    private var _isExpect: Boolean = isExpect

    override var isExpect: Boolean
        get() = _isExpect
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate()
            }
        }

    private var _isFun: Boolean = isFun

    override var isFun: Boolean
        get() = _isFun
        set(value) {
            if (_isFun != value) {
                _isFun = value
                invalidate()
            }
        }

    private var _hasEnumEntries: Boolean = hasEnumEntries

    override var hasEnumEntries: Boolean
        get() = _hasEnumEntries
        set(value) {
            if (_hasEnumEntries != value) {
                _hasEnumEntries = value
                invalidate()
            }
        }

    private var _thisReceiver: BirValueParameter? = thisReceiver

    override var thisReceiver: BirValueParameter?
        get() = _thisReceiver
        set(value) {
            if (_thisReceiver != value) {
                replaceChild(_thisReceiver, value)
                _thisReceiver = value
                invalidate()
            }
        }

    private var _valueClassRepresentation: ValueClassRepresentation<BirSimpleType>? =
            valueClassRepresentation

    override var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?
        get() = _valueClassRepresentation
        set(value) {
            if (_valueClassRepresentation != value) {
                _valueClassRepresentation = value
                invalidate()
            }
        }
    init {
        initChild(_thisReceiver)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        when {
            this._thisReceiver === old -> this.thisReceiver = new as BirValueParameter
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> = when(id) {
        0 -> this.typeParameters
        1 -> this.declarations
        else -> throwChildrenListWithIdNotFound(id)
    }
}
