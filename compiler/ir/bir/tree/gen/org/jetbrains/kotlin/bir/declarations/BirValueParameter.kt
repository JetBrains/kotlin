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
import org.jetbrains.kotlin.bir.symbols.BirValueParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.ir.util.IdSignature

interface BirValueParameter : BirDeclarationBase, BirValueDeclaration {
    val isAssignable: Boolean

    override val symbol: BirValueParameterSymbol

    var index: Int

    var varargElementType: BirType?

    var isCrossinline: Boolean

    var isNoinline: Boolean

    var isHidden: Boolean

    var defaultValue: BirExpressionBody?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        defaultValue?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirValueParameter

    companion object : BirElementClass<BirValueParameter>(BirValueParameter::class.java, 100, true)
}
