/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation

interface BirClass : BirDeclarationBase, BirPossiblyExternalDeclaration, BirDeclarationWithVisibility, BirTypeParametersContainer, BirDeclarationContainer, BirAttributeContainer, BirMetadataSourceOwner {
    override val symbol: BirClassSymbol

    var kind: ClassKind

    var modality: Modality

    var isCompanion: Boolean

    var isInner: Boolean

    var isData: Boolean

    var isValue: Boolean

    var isExpect: Boolean

    var isFun: Boolean

    var hasEnumEntries: Boolean

    val source: SourceElement

    var superTypes: List<BirType>

    var thisReceiver: BirValueParameter?

    var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        typeParameters.acceptChildren(visitor, data)
        declarations.acceptChildren(visitor, data)
        thisReceiver?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirClass

    companion object : BirElementClass<BirClass>(BirClass::class.java, 12, true)
}
