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
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

private val SCRIPT_ORIGIN = object : IrDeclarationOriginImpl("SCRIPT") {}

class IrScriptImpl(
    override val symbol: IrScriptSymbol,
    override val name: Name
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

    override lateinit var thisReceiver: IrValueParameter

    override lateinit var baseClass: IrType
    override lateinit var explicitCallParameters: List<IrValueParameter>
    override lateinit var implicitReceivers: List<IrValueParameter>
    override lateinit var providedProperties: List<IrPropertySymbol>
    override var resultProperty: IrPropertySymbol? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ScriptDescriptor
        get() = symbol.descriptor

    override val factory: IrFactory
        get() = error("Create IrScriptImpl directly")

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitScript(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
        thisReceiver.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        statements.transformInPlace(transformer, data)
        thisReceiver = thisReceiver.transform(transformer, data)
    }
}
