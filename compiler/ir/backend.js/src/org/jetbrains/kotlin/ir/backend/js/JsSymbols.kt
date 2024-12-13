/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.JsStandardClassIds.BASE_JS_PACKAGE
import org.jetbrains.kotlin.name.Name

abstract class JsCommonSymbols(
    irBuiltIns: IrBuiltIns,
) : Symbols(irBuiltIns) {
    @OptIn(InternalSymbolFinderAPI::class)
    val coroutineSymbols = JsCommonCoroutineSymbols(irBuiltIns.symbolFinder)
}

@OptIn(ObsoleteDescriptorBasedAPI::class, InternalSymbolFinderAPI::class)
class JsSymbols(
    irBuiltIns: IrBuiltIns,
    private val stageController: StageController,
    private val intrinsics: JsIntrinsics,
) : JsCommonSymbols(irBuiltIns) {
    override val throwNullPointerException =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_NPE")

    init {
        symbolFinder.topLevelFunction(kotlinPackageFqn, "noWhenBranchMatchedException")
    }

    override val throwTypeCastException =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_CCE")

    override val throwUninitializedPropertyAccessException =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "throwUninitializedPropertyAccessException")

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "throwKotlinNothingValueException")

    override val defaultConstructorMarker =
        symbolFinder.topLevelClass(BASE_JS_PACKAGE, "DefaultConstructorMarker")

    override val throwISE: IrSimpleFunctionSymbol =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_ISE")

    override val throwIAE: IrSimpleFunctionSymbol =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_IAE")

    override val stringBuilder
        get() = TODO("not implemented")
    override val coroutineImpl =
        coroutineSymbols.coroutineImpl
    override val coroutineSuspendedGetter =
        coroutineSymbols.coroutineSuspendedGetter

    private val _arraysContentEquals = symbolFinder.topLevelFunctions(COLLECTIONS_PACKAGE_FQ_NAME, "contentEquals").filter {
        it.descriptor.extensionReceiverParameter?.type?.isMarkedNullable == true
    }

    // Can't use .owner until ExternalStubGenerator is invoked, hence get() = here.
    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
        get() = _arraysContentEquals.associateBy { it.owner.parameters[0].type.makeNotNull() }

    override val getContinuation = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, "getContinuation")

    override val continuationClass = coroutineSymbols.continuationClass

    override val coroutineContextGetter = coroutineSymbols.coroutineContextGetter

    override val suspendCoroutineUninterceptedOrReturn = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, COROUTINE_SUSPEND_OR_RETURN_JS_NAME)

    override val coroutineGetContext = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, GET_COROUTINE_CONTEXT_NAME)

    override val returnIfSuspended = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, "returnIfSuspended")

    override val functionAdapter = symbolFinder.topLevelClass(BASE_JS_PACKAGE, "FunctionAdapter")

    override fun functionN(n: Int): IrClassSymbol {
        return stageController.withInitialIr { super.functionN(n) }
    }

    override fun suspendFunctionN(n: Int): IrClassSymbol {
        return stageController.withInitialIr { super.suspendFunctionN(n) }
    }


    private val getProgressionLastElementSymbols =
        symbolFinder.findFunctions(Name.identifier("getProgressionLastElement"), "kotlin", "internal")

    override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        getProgressionLastElementSymbols.associateBy { it.owner.returnType.classifierOrFail }
    }

    private val toUIntSymbols = symbolFinder.findFunctions(Name.identifier("toUInt"), "kotlin")

    override val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        toUIntSymbols.associateBy {
            it.owner.parameters[0].type.classifierOrFail
        }
    }

    private val toULongSymbols = symbolFinder.findFunctions(Name.identifier("toULong"), "kotlin")

    override val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        toULongSymbols.associateBy {
            it.owner.parameters[0].type.classifierOrFail
        }
    }

    override fun isSideEffectFree(call: IrCall): Boolean =
        call.symbol in intrinsics.primitiveToLiteralConstructor.values ||
                call.symbol == intrinsics.arrayLiteral ||
                call.symbol == intrinsics.arrayConcat ||
                call.symbol == intrinsics.jsBoxIntrinsic ||
                call.symbol == intrinsics.jsUnboxIntrinsic

    companion object {
        // TODO: due to name clash those weird suffix is required, remove it once `MemberNameGenerator` is implemented
        private const val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
        private const val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
    }
}