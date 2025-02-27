/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.FqName

class IrExternalPackageFragmentImpl(
    symbol: IrExternalPackageFragmentSymbol,
    packageFqName: FqName,
) : IrExternalPackageFragment() {
    override var startOffset: Int
        get() = UNDEFINED_OFFSET
        set(value) {
            error("Mutation of startOffset is not supported for this class.")
        }

    override var endOffset: Int
        get() = UNDEFINED_OFFSET
        set(value) {
            error("Mutation of endOffset is not supported for this class.")
        }

    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration> by declarationsAttribute
    override var packageFqName: FqName by packageFqNameAttribute
    override val symbol: IrExternalPackageFragmentSymbol by symbolAttribute

    init {
        preallocateStorage(5)
        initAttribute(packageFqNameAttribute, packageFqName)
        initAttribute(declarationsAttribute, ArrayList())
        initAttribute(symbolAttribute, symbol)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrExternalPackageFragmentImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val declarationsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrDeclaration>>(IrExternalPackageFragmentImpl::class.java, 11, "declarations", null)
        @JvmStatic private val packageFqNameAttribute = IrIndexBasedAttributeRegistry.createAttr<FqName>(IrExternalPackageFragmentImpl::class.java, 4, "packageFqName", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExternalPackageFragmentSymbol>(IrExternalPackageFragmentImpl::class.java, 12, "symbol", null)
    }
}
