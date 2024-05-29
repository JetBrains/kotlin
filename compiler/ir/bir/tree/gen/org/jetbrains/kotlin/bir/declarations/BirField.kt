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
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

interface BirField : BirDeclarationBase, BirPossiblyExternalDeclaration, BirDeclarationWithVisibility, BirDeclarationParent, BirMetadataSourceOwner {
    override val symbol: BirFieldSymbol

    var type: BirType

    var isFinal: Boolean

    var isStatic: Boolean

    var initializer: BirExpressionBody?

    var correspondingPropertySymbol: BirPropertySymbol?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        initializer?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirField

    companion object : BirElementClass<BirField>(BirField::class.java, 45, true)
}
