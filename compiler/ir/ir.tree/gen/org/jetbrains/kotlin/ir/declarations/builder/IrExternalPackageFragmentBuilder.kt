/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.name.FqName

/**
 * Collects the properties of a [IrExternalPackageFragment] and builds one.
 *
 * A property with no sensible default is declared `lateinit`, so building without it throws rather than
 * inventing a value. There is no way to express "required" for a property the caller assigns inside a
 * lambda; a constructor parameter could, at the cost of the property being settable only once.
 */
@IrBuilderDsl
class IrExternalPackageFragmentBuilder {
    lateinit var packageFqName: FqName
    lateinit var module: IrModuleFragment
    var symbol: IrExternalPackageFragmentSymbol = IrExternalPackageFragmentSymbolImpl()

    @PublishedApi
    internal fun build(): IrExternalPackageFragment {
        val result = IrExternalPackageFragmentImpl(
            packageFqName = packageFqName,
            module = module,
            symbol = symbol,
        )
        return result
    }

    /**
     * Copies the properties of [from] that identify a *kind* of IrExternalPackageFragment, not a particular one,
     * including its source range.
     *
     * Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both
     * to one, and sharing a body or a receiver would splice one tree into another. Properties the element
     * declares `lateinit` are skipped too, since reading one that was never set throws.
     */
    fun updateFrom(from: IrExternalPackageFragment) {
        packageFqName = from.packageFqName
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildExternalPackageFragment(init: IrExternalPackageFragmentBuilder.() -> Unit): IrExternalPackageFragment {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return IrExternalPackageFragmentBuilder().apply(init).build()
}
