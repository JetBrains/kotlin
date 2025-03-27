/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.backend.common.lower.AbstractPropertyReferenceLowering
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmSymbols
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources.File as PLFile

internal class PropertyReferencesConstructorsSet(
    val local: IrConstructorSymbol,
    val byReceiversCount: List<IrConstructorSymbol>
) {
    constructor(local: IrClassSymbol, byReceiversCount: List<IrClassSymbol>) : this(
        local.constructors.single(),
        byReceiversCount.map { it.constructors.single() }
    )
}

internal val WasmSymbols.immutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
        kLocalDelegatedPropertyImpl,
        listOf(kProperty0Impl, kProperty1Impl, kProperty2Impl)
    )

internal val WasmSymbols.mutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
        kLocalDelegatedMutablePropertyImpl,
        listOf(kMutableProperty0Impl, kMutableProperty1Impl, kMutableProperty2Impl)
    )

class WasmPropertyReferenceLowering(context: WasmBackendContext) : AbstractPropertyReferenceLowering<WasmBackendContext>(context) {
    private val symbols = context.symbols
    private val immutableSymbols = symbols.immutablePropertiesConstructors
    private val mutableSymbols = symbols.mutablePropertiesConstructors

    override fun functionReferenceClass(arity: Int): IrClassSymbol {
        return symbols.functionN(arity)
    }

    override fun IrBuilderWithScope.createKProperty(
        reference: IrRichPropertyReference,
        typeArguments: List<IrType>,
        name: String?,
        getterReference: IrRichFunctionReference,
        setterReference: IrRichFunctionReference?,
    ): IrExpression {
        val constructor = if (setterReference != null) {
            mutableSymbols
        } else {
            immutableSymbols
        }.byReceiversCount[typeArguments.size - 1]

        return irCall(constructor, reference.type, typeArguments).apply {
            arguments[0] = name?.let(::irString) ?: irNull()
            arguments[1] = reference.reflectionTargetLinkageError?.let {
                this@WasmPropertyReferenceLowering.context.partialLinkageSupport.prepareLinkageError(
                    doNotLog = false,
                    it,
                    reference,
                    PLFile.determineFileFor(reference.getterFunction),
                )
            }?.let(::irString) ?: irNull()
            arguments[2] = getterReference
            setterReference?.let { arguments[3] = it }
        }
    }

    override fun IrBuilderWithScope.createLocalKProperty(
        reference: IrRichPropertyReference,
        propertyName: String,
        propertyType: IrType,
    ): IrExpression {
        val constructor = if (reference.setterFunction != null) mutableSymbols.local else immutableSymbols.local

        return irCall(
            callee = constructor,
            type = constructor.owner.returnType,
            typeArguments = listOf(reference.type)
        ).apply {
            arguments[0] = irString(propertyName)
        }
    }
}