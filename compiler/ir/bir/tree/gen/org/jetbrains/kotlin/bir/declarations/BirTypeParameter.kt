/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirImplElementBase
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.types.Variance

abstract class BirTypeParameter() : BirImplElementBase(), BirDeclarationBase, BirDeclarationWithName {
    abstract override val symbol: BirTypeParameterSymbol

    abstract var variance: Variance

    abstract var index: Int

    abstract var isReified: Boolean

    abstract var superTypes: List<BirType>

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirTypeParameter

    companion object : BirElementClass<BirTypeParameter>(BirTypeParameter::class.java, 96, true)
}
