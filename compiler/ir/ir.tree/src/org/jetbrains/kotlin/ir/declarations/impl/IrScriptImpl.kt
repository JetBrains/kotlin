/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

private val SCRIPT_ORIGIN = object : IrDeclarationOriginImpl("FIELD_FOR_OBJECT_INSTANCE") {}

class IrScriptImpl(
    override val symbol: IrScriptSymbol,
    override val name: Name
) : IrScript, IrDeclaration {
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
    override val metadata: MetadataSource? get() = null

    override val declarations: MutableList<IrDeclaration> = mutableListOf()
    override val statements: MutableList<IrStatement> = mutableListOf()

    override lateinit var thisReceiver: IrValueParameter

    @DescriptorBasedIr
    override val descriptor: ScriptDescriptor = symbol.descriptor

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitScript(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
        statements.forEach { it.accept(visitor, data) }
        thisReceiver.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        declarations.transform { it.transform(transformer, data) }
        statements.transform { it.transform(transformer, data) }
        thisReceiver = thisReceiver.transform(transformer, data)
    }
}
