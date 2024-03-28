/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.symbols.BirTypeAliasSymbol
import org.jetbrains.kotlin.bir.types.BirType

interface BirTypeAlias : BirElement, BirDeclaration, BirDeclarationWithName, BirDeclarationWithVisibility, BirTypeParametersContainer, BirTypeAliasSymbol {
    var isActual: Boolean
    var expandedType: BirType

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        typeParameters.acceptChildren(visitor, data)
    }

    companion object : BirElementClass<BirTypeAlias>(BirTypeAlias::class.java, 60, true)
}
