/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

private val SCRIPT_ORIGIN = object : IrDeclarationOriginImpl("SCRIPT") {}

class IrScriptImpl(
    override val symbol: IrScriptSymbol,
    override var name: Name,
    override val factory: IrFactory,
) : IrScript() {
    override val startOffset: Int get() = UNDEFINED_OFFSET
    override val endOffset: Int get() = UNDEFINED_OFFSET
    override var origin: IrDeclarationOrigin = SCRIPT_ORIGIN

    private var _parent: IrDeclarationParent? = null
    override var parent: IrDeclarationParent
        get() = _parent
            ?: throw UninitializedPropertyAccessException("Parent not initialized: $this")
        set(v) {
            _parent = v
        }

    override var annotations: List<IrConstructorCall> = SmartList()

    override val statements: MutableList<IrStatement> = mutableListOf()

    override var metadata: MetadataSource? = null

    override lateinit var thisReceiver: IrValueParameter

    override lateinit var baseClass: IrType
    override lateinit var explicitCallParameters: List<IrValueParameter>
    override lateinit var implicitReceiversParameters: List<IrValueParameter>
    override lateinit var providedProperties: List<Pair<IrValueParameter, IrPropertySymbol>>
    override var resultProperty: IrPropertySymbol? = null
    override var earlierScriptsParameter: IrValueParameter? = null
    override var earlierScripts: List<IrScriptSymbol>? = null
    override var targetClass: IrClassSymbol? = null
    override var constructor: IrConstructor? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ScriptDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }
}
