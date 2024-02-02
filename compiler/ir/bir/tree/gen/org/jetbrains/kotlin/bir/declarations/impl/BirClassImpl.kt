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

    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan = sourceSpan

    override var signature: IdSignature? = signature

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override var isExternal: Boolean = isExternal

    override var visibility: DescriptorVisibility = visibility

    override var attributeOwnerId: BirAttributeContainer = this

    override var kind: ClassKind = kind

    override var modality: Modality = modality

    override var isCompanion: Boolean = isCompanion

    override var isInner: Boolean = isInner

    override var isData: Boolean = isData

    override var isValue: Boolean = isValue

    override var isExpect: Boolean = isExpect

    override var isFun: Boolean = isFun

    /**
     * Returns true iff this is a class loaded from dependencies which has the `HAS_ENUM_ENTRIES` metadata flag set.
     * This flag is useful for Kotlin/JVM to determine whether an enum class from dependency actually has the `entries` property
     * in its bytecode, as opposed to whether it has it in its member scope, which is true even for enum classes compiled by
     * old versions of Kotlin which did not support the EnumEntries language feature.
     */
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
            1 -> this.annotations
            2 -> this.typeParameters
            3 -> this.declarations
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
