/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.class]
 */
abstract class BirClass : BirElementBase(), BirDeclaration, BirPossiblyExternalDeclaration,
        BirDeclarationWithVisibility, BirTypeParametersContainer, BirDeclarationContainer,
        BirAttributeContainer, BirMetadataSourceOwner, BirClassSymbol {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor?

    abstract var kind: ClassKind

    abstract var modality: Modality

    abstract var isCompanion: Boolean

    abstract var isInner: Boolean

    abstract var isData: Boolean

    abstract var isValue: Boolean

    abstract var isExpect: Boolean

    abstract var isFun: Boolean

    /**
     * Returns true iff this is a class loaded from dependencies which has the `HAS_ENUM_ENTRIES`
     * metadata flag set.
     * This flag is useful for Kotlin/JVM to determine whether an enum class from dependency
     * actually has the `entries` property
     * in its bytecode, as opposed to whether it has it in its member scope, which is true even for
     * enum classes compiled by
     * old versions of Kotlin which did not support the EnumEntries language feature.
     */
    abstract var hasEnumEntries: Boolean

    abstract val source: SourceElement

    abstract var superTypes: List<BirType>

    abstract var thisReceiver: BirValueParameter?

    abstract var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        typeParameters.acceptChildren(visitor, data)
        declarations.acceptChildren(visitor, data)
        thisReceiver?.accept(data, visitor)
    }

    companion object
}
