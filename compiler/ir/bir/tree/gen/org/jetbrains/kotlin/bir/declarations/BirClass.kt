/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.class]
 */
interface BirClass : BirDeclaration, BirPossiblyExternalDeclaration,
        BirDeclarationWithVisibility, BirTypeParametersContainer, BirDeclarationContainer,
        BirAttributeContainer, BirMetadataSourceOwner, BirClassSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor?

    var kind: ClassKind

    var modality: Modality

    var isCompanion: Boolean

    var isInner: Boolean

    var isData: Boolean

    var isValue: Boolean

    var isExpect: Boolean

    var isFun: Boolean

    /**
     * Returns true iff this is a class loaded from dependencies which has the `HAS_ENUM_ENTRIES`
     * metadata flag set.
     * This flag is useful for Kotlin/JVM to determine whether an enum class from dependency
     * actually has the `entries` property
     * in its bytecode, as opposed to whether it has it in its member scope, which is true even for
     * enum classes compiled by
     * old versions of Kotlin which did not support the EnumEntries language feature.
     */
    var hasEnumEntries: Boolean

    val source: SourceElement

    var superTypes: List<BirType>

    var thisReceiver: BirValueParameter?

    var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        typeParameters.acceptChildren(visitor, data)
        declarations.acceptChildren(visitor, data)
        thisReceiver?.accept(data, visitor)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        typeParameters.acceptChildrenLite(visitor)
        declarations.acceptChildrenLite(visitor)
        thisReceiver?.acceptLite(visitor)
    }

    companion object
}
