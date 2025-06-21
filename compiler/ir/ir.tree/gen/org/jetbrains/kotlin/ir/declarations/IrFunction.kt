/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.function]
 */
sealed class IrFunction : IrDeclarationBase(), IrPossiblyExternalDeclaration, IrDeclarationWithVisibility, IrTypeParametersContainer, IrSymbolOwner, IrDeclarationParent, IrReturnTarget, IrMemberWithContainerSource, IrMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: FunctionDescriptor

    abstract override val symbol: IrFunctionSymbol

    abstract var isInline: Boolean

    abstract var isExpect: Boolean

    abstract var returnType: IrType

    abstract var body: IrBody?

    private val _parameters: MutableList<IrValueParameter> = ArrayList()

    /**
     * All value parameters.
     *
     * Parameters must follow this order:
     *
     * [[dispatch receiver, context parameters, extension receiver, regular parameters]].
     */
    @OptIn(DelicateIrParameterIndexSetter::class)
    var parameters: List<IrValueParameter>
        get() = _parameters
        set(value) {
            for (parameter in _parameters) {
                parameter.indexInParameters = -1
            }
            for ((index, parameter) in value.withIndex()) {
                parameter.indexInParameters = index
            }
            _parameters.assignFrom(value.toList())
        }

    /**
     * The first parameter of kind [IrParameterKind.DispatchReceiver] in [parameters], if present.
     */
    val dispatchReceiverParameter: IrValueParameter?
        get() = parameters.firstOrNull()?.takeIf { it.kind == IrParameterKind.DispatchReceiver }

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        parameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
        parameters = parameters.transformIfNeeded(transformer, data)
        body = body?.transform(transformer, data)
    }
}
