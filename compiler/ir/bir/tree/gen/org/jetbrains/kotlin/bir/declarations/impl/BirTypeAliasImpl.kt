/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "CanBePrimaryConstructorProperty")

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirTypeAlias
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirTypeAliasSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirTypeAliasImpl(
    sourceSpan: SourceSpan,
    signature: IdSignature?,
    annotations: List<BirConstructorCall>,
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: DescriptorVisibility,
    isActual: Boolean,
    expandedType: BirType,
) : BirImplElementBase(), BirTypeAlias, BirTypeAliasSymbol {
    constructor(
        sourceSpan: SourceSpan,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isActual: Boolean,
        expandedType: BirType,
    ) : this(
        sourceSpan = sourceSpan,
        signature = null,
        annotations = emptyList(),
        origin = origin,
        name = name,
        visibility = visibility,
        isActual = isActual,
        expandedType = expandedType,
    )

    override val owner: BirTypeAliasImpl
        get() = this

    override val isBound: Boolean
        get() = true

    override var sourceSpan: SourceSpan = sourceSpan

    override val signature: IdSignature? = signature

    override var annotations: List<BirConstructorCall> = annotations

    override var origin: IrDeclarationOrigin = origin

    override var name: Name = name

    override var visibility: DescriptorVisibility = visibility

    override val symbol: BirTypeAliasSymbol
        get() = this

    override var isActual: Boolean = isActual

    override var expandedType: BirType = expandedType

    override val typeParameters: BirImplChildElementList<BirTypeParameter> = BirImplChildElementList(this, 1, false)


    init {
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        typeParameters.acceptChildrenLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.typeParameters
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
