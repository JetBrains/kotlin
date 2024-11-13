/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.isNullable

abstract class JsCommonSymbols(
    module: ModuleDescriptor,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
) : Symbols(irBuiltIns) {
    val coroutineSymbols = JsCommonCoroutineSymbols(symbolTable, module)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsSymbols(
    private val context: JsIrBackendContext,
    symbolTable: SymbolTable,
) : JsCommonSymbols(context.module, context.irBuiltIns, symbolTable) {
    override val throwNullPointerException =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_NPE"))).single())

    init {
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(kotlinPackageFqn.child(Name.identifier("noWhenBranchMatchedException"))).single())
    }

    override val throwTypeCastException =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_CCE"))).single())

    override val throwUninitializedPropertyAccessException =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(FqName("kotlin.throwUninitializedPropertyAccessException")).single())

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(FqName("kotlin.throwKotlinNothingValueException")).single())

    override val defaultConstructorMarker =
        symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("DefaultConstructorMarker"))

    override val throwISE: IrSimpleFunctionSymbol =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_ISE"))).single())

    override val throwIAE: IrSimpleFunctionSymbol =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_IAE"))).single())

    override val stringBuilder
        get() = TODO("not implemented")
    override val coroutineImpl =
        coroutineSymbols.coroutineImpl
    override val coroutineSuspendedGetter =
        coroutineSymbols.coroutineSuspendedGetter

    private val _arraysContentEquals = context.getFunctions(FqName("kotlin.collections.contentEquals")).mapNotNull {
        if (it.extensionReceiverParameter != null && it.extensionReceiverParameter!!.type.isNullable())
            symbolTable.descriptorExtension.referenceSimpleFunction(it)
        else null
    }

    // Can't use .owner until ExternalStubGenerator is invoked, hence get() = here.
    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
        get() = _arraysContentEquals.associateBy { it.owner.extensionReceiverParameter!!.type.makeNotNull() }

    override val getContinuation = symbolTable.descriptorExtension.referenceSimpleFunction(context.getJsInternalFunction("getContinuation"))

    override val continuationClass = coroutineSymbols.continuationClass

    override val coroutineContextGetter =
        symbolTable.descriptorExtension.referenceSimpleFunction(coroutineSymbols.coroutineContextProperty.getter!!)

    override val suspendCoroutineUninterceptedOrReturn =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getJsInternalFunction(COROUTINE_SUSPEND_OR_RETURN_JS_NAME))

    override val coroutineGetContext =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getJsInternalFunction(GET_COROUTINE_CONTEXT_NAME))

    override val returnIfSuspended =
        symbolTable.descriptorExtension.referenceSimpleFunction(context.getJsInternalFunction("returnIfSuspended"))

    override val functionAdapter =
        symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("FunctionAdapter"))

    override fun functionN(n: Int): IrClassSymbol {
        return context.irFactory.stageController.withInitialIr { super.functionN(n) }
    }

    override fun suspendFunctionN(n: Int): IrClassSymbol {
        return context.irFactory.stageController.withInitialIr { super.suspendFunctionN(n) }
    }


    private val getProgressionLastElementSymbols =
        irBuiltIns.findFunctions(Name.identifier("getProgressionLastElement"), "kotlin", "internal")

    override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        getProgressionLastElementSymbols.associateBy { it.owner.returnType.classifierOrFail }
    }

    private val toUIntSymbols = irBuiltIns.findFunctions(Name.identifier("toUInt"), "kotlin")

    override val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        toUIntSymbols.associateBy {
            it.owner.extensionReceiverParameter?.type?.classifierOrFail
                ?: irError("Expected extension receiver for") {
                    withIrEntry("it.owner", it.owner)
                }
        }
    }

    private val toULongSymbols = irBuiltIns.findFunctions(Name.identifier("toULong"), "kotlin")

    override val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        toULongSymbols.associateBy {
            it.owner.extensionReceiverParameter?.type?.classifierOrFail
                ?: irError("Expected extension receiver for") {
                    withIrEntry("it.owner", it.owner)
                }
        }
    }

    override fun isSideEffectFree(call: IrCall): Boolean =
        call.symbol in context.intrinsics.primitiveToLiteralConstructor.values ||
                call.symbol == context.intrinsics.arrayLiteral ||
                call.symbol == context.intrinsics.arrayConcat ||
                call.symbol == context.intrinsics.jsBoxIntrinsic ||
                call.symbol == context.intrinsics.jsUnboxIntrinsic

    companion object {
        // TODO: due to name clash those weird suffix is required, remove it once `MemberNameGenerator` is implemented
        private const val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
        private const val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
    }
}