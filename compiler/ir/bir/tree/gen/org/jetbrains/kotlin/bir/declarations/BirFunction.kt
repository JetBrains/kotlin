/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.symbols.BirFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirType

interface BirFunction : BirDeclarationBase, BirPossiblyExternalDeclaration, BirDeclarationWithVisibility, BirTypeParametersContainer, BirSymbolOwner, BirDeclarationParent, BirReturnTarget, BirMemberWithContainerSource, BirMetadataSourceOwner {
    override val symbol: BirFunctionSymbol

    var isInline: Boolean

    var isExpect: Boolean

    var returnType: BirType

    var dispatchReceiverParameter: BirValueParameter?

    var extensionReceiverParameter: BirValueParameter?

    val valueParameters: BirChildElementList<BirValueParameter>

    var contextReceiverParametersCount: Int

    var body: BirBody?

    companion object : BirElementClass<BirFunction>(BirFunction::class.java, 48, false)
}
