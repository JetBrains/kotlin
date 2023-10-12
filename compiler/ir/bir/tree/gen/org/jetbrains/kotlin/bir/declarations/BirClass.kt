/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.class]
 */
abstract class BirClass : BirDeclarationBase(), BirPossiblyExternalDeclaration,
        BirDeclarationWithVisibility, BirTypeParametersContainer, BirDeclarationContainer,
        BirAttributeContainer, BirMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor

    abstract override val symbol: BirClassSymbol

    abstract var kind: ClassKind

    abstract var modality: Modality

    abstract var isCompanion: Boolean

    abstract var isInner: Boolean

    abstract var isData: Boolean

    abstract var isValue: Boolean

    abstract var isExpect: Boolean

    abstract var isFun: Boolean

    abstract val source: SourceElement

    abstract var superTypes: List<BirType>

    abstract var thisReceiver: BirValueParameter?

    abstract var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?

    /**
     * If this is a sealed class or interface, this list contains symbols of all its immediate
     * subclasses.
     * Otherwise, this is an empty list.
     *
     * NOTE: If this [BirClass] was deserialized from a klib, this list will always be empty!
     * See [KT-54028](https://youtrack.jetbrains.com/issue/KT-54028).
     */
    abstract var sealedSubclasses: List<BirClassSymbol>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        typeParameters.forEach { it.accept(data, visitor) }
        declarations.forEach { it.accept(data, visitor) }
        thisReceiver?.accept(data, visitor)
    }
}
