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

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override var isExternal: Boolean = isExternal

    override var visibility: DescriptorVisibility = visibility

    override var attributeOwnerId: BirAttributeContainer = this

    override val symbol: BirClassSymbol
        get() = this

    override var kind: ClassKind = kind

    override var modality: Modality = modality

    override var isCompanion: Boolean = isCompanion

    override var isInner: Boolean = isInner

    override var isData: Boolean = isData

    override var isValue: Boolean = isValue

    override var isExpect: Boolean = isExpect

    override var isFun: Boolean = isFun

    override var hasEnumEntries: Boolean = hasEnumEntries

    override val source: SourceElement = source

    override var superTypes: List<BirType> = superTypes

    private var _thisReceiver: BirValueParameter? = thisReceiver
    override var thisReceiver: BirValueParameter?
        get() {
            return _thisReceiver
        }
        set(value) {
            if (_thisReceiver !== value) {
                childReplaced(_thisReceiver, value)
                _thisReceiver = value
            }
        }

    override var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>? = valueClassRepresentation

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
