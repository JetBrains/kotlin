/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirImplElementBase
import org.jetbrains.kotlin.bir.symbols.BirPackageFragmentSymbol
import org.jetbrains.kotlin.name.FqName

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.packageFragment]
 */
abstract class BirPackageFragment(elementClass: BirElementClass<*>) : BirImplElementBase(elementClass), BirElement, BirDeclarationContainer, BirSymbolOwner, BirPackageFragmentSymbol {
    abstract var packageFqName: FqName

    companion object : BirElementClass<BirPackageFragment>(BirPackageFragment::class.java, 93, false)
}
