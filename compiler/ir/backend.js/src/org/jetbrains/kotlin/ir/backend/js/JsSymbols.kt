/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.JsStandardClassIds.BASE_JS_PACKAGE
import org.jetbrains.kotlin.name.Name

abstract class JsCommonSymbols(
    irBuiltIns: IrBuiltIns,
) : Symbols(irBuiltIns) {
    val coroutineSymbols = JsCommonCoroutineSymbols(irBuiltIns)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsSymbols(
    private val context: JsIrBackendContext,
) : JsCommonSymbols(context.irBuiltIns) {
    override val throwNullPointerException =
        irBuiltIns.topLevelFunction(kotlinPackageFqn, "THROW_NPE")

    init {
        irBuiltIns.topLevelFunction(kotlinPackageFqn, "noWhenBranchMatchedException")
    }

    override val throwTypeCastException =
        irBuiltIns.topLevelFunction(kotlinPackageFqn, "THROW_CCE")

    override val throwUninitializedPropertyAccessException =
        irBuiltIns.topLevelFunction(kotlinPackageFqn, "throwUninitializedPropertyAccessException")

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
        irBuiltIns.topLevelFunction(kotlinPackageFqn, "throwKotlinNothingValueException")

    override val defaultConstructorMarker =
        irBuiltIns.topLevelClass(BASE_JS_PACKAGE, "DefaultConstructorMarker")

    override val throwISE: IrSimpleFunctionSymbol =
        irBuiltIns.topLevelFunction(kotlinPackageFqn, "THROW_ISE")

    override val throwIAE: IrSimpleFunctionSymbol =
        irBuiltIns.topLevelFunction(kotlinPackageFqn, "THROW_IAE")

    override val stringBuilder
        get() = TODO("not implemented")
    override val coroutineImpl =
        coroutineSymbols.coroutineImpl
    override val coroutineSuspendedGetter =
        coroutineSymbols.coroutineSuspendedGetter

    private val _arraysContentEquals = irBuiltIns.topLevelFunctions(COLLECTIONS_PACKAGE_FQ_NAME, "contentEquals").filter {
        it.descriptor.extensionReceiverParameter?.type?.isMarkedNullable == true
    }

    // Can't use .owner until ExternalStubGenerator is invoked, hence get() = here.
    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
        get() = _arraysContentEquals.associateBy { it.owner.extensionReceiverParameter!!.type.makeNotNull() }

    override val getContinuation = irBuiltIns.topLevelFunction(BASE_JS_PACKAGE, "getContinuation")

    override val continuationClass = coroutineSymbols.continuationClass

    override val coroutineContextGetter = coroutineSymbols.coroutineContextGetter

    override val suspendCoroutineUninterceptedOrReturn = irBuiltIns.topLevelFunction(BASE_JS_PACKAGE, COROUTINE_SUSPEND_OR_RETURN_JS_NAME)

    override val coroutineGetContext = irBuiltIns.topLevelFunction(BASE_JS_PACKAGE, GET_COROUTINE_CONTEXT_NAME)

    override val returnIfSuspended = irBuiltIns.topLevelFunction(BASE_JS_PACKAGE, "returnIfSuspended")

    override val functionAdapter = irBuiltIns.topLevelClass(BASE_JS_PACKAGE, "FunctionAdapter")

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