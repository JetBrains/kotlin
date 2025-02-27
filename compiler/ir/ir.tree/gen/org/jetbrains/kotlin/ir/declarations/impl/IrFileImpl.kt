/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.FqName

class IrFileImpl(
    fileEntry: IrFileEntry,
    symbol: IrFileSymbol,
    packageFqName: FqName,
) : IrFile() {
    override var startOffset: Int
        get() = 0
        set(value) {
            error("Mutation of startOffset is not supported for this class.")
        }

    override var endOffset: Int
        get() = fileEntry.maxOffset
        set(value) {
            error("Mutation of endOffset is not supported for this class.")
        }

    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration> by declarationsAttribute
    override var packageFqName: FqName by packageFqNameAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var metadata: MetadataSource? by metadataAttribute
    override val symbol: IrFileSymbol by symbolAttribute
    override var _module: IrModuleFragment? by _moduleAttribute
    override var fileEntry: IrFileEntry by fileEntryAttribute

    init {
        preallocateStorage(7)
        initAttribute(packageFqNameAttribute, packageFqName)
        initAttribute(fileEntryAttribute, fileEntry)
        initAttribute(declarationsAttribute, ArrayList())
        initAttribute(symbolAttribute, symbol)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrFileImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val declarationsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrDeclaration>>(IrFileImpl::class.java, 11, "declarations", null)
        @JvmStatic private val packageFqNameAttribute = IrIndexBasedAttributeRegistry.createAttr<FqName>(IrFileImpl::class.java, 4, "packageFqName", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrFileImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrFileImpl::class.java, 13, "metadata", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFileSymbol>(IrFileImpl::class.java, 12, "symbol", null)
        @JvmStatic private val _moduleAttribute = IrIndexBasedAttributeRegistry.createAttr<IrModuleFragment?>(IrFileImpl::class.java, 5, "_module", null)
        @JvmStatic private val fileEntryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFileEntry>(IrFileImpl::class.java, 6, "fileEntry", null)
    }

    override var module: IrModuleFragment
        get() = _module!!
        set(value) { _module = value }

    internal val isInsideModule: Boolean
        get() = _module != null
}
