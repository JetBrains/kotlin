/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrScriptImpl(
    symbol: IrScriptSymbol,
    override var name: Name,
    override val factory: IrFactory,
    startOffset: Int,
    endOffset: Int,
) : IrScript(
    startOffset = startOffset,
    endOffset = endOffset,
    origin = SCRIPT_ORIGIN,
    symbol = symbol,
) {
    override var annotations: List<IrConstructorCall> = emptyList()

    override lateinit var parent: IrDeclarationParent

    override val statements: MutableList<IrStatement> = ArrayList()

    override var metadata: MetadataSource? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ScriptDescriptor
        get() = symbol.descriptor

    override var thisReceiver: IrValueParameter? = null

    override var baseClass: IrType? = null

    override lateinit var explicitCallParameters: List<IrVariable>

    override lateinit var implicitReceiversParameters: List<IrValueParameter>

    override lateinit var providedProperties: List<IrPropertySymbol>

    override lateinit var providedPropertiesParameters: List<IrValueParameter>

    override var resultProperty: IrPropertySymbol? = null

    override var earlierScriptsParameter: IrValueParameter? = null

    override var importedScripts: List<IrScriptSymbol>? = null

    override var earlierScripts: List<IrScriptSymbol>? = null

    override var targetClass: IrClassSymbol? = null

    override var constructor: IrConstructor? = null

    init {
        symbol.bind(this)
    }
}
