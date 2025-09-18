/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.PreSerializationJsSymbols
import org.jetbrains.kotlin.backend.common.ir.PreSerializationWebSymbols
import org.jetbrains.kotlin.backend.common.ir.KlibSymbols
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.TEXT_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds.BASE_JS_PACKAGE

// TODO KT-77388 rename to `BackendWebSymbolsImpl`
@OptIn(InternalSymbolFinderAPI::class)
abstract class JsCommonSymbols(
    irBuiltIns: IrBuiltIns,
) : PreSerializationWebSymbols, KlibSymbols(irBuiltIns) {
    override val coroutineImpl: IrClassSymbol = symbolFinder.topLevelClass(COROUTINE_PACKAGE_FQNAME, COROUTINE_IMPL_NAME.asString())
    override val continuationClass = symbolFinder.topLevelClass(COROUTINE_PACKAGE_FQNAME, CONTINUATION_NAME.asString())
    override val coroutineSuspendedGetter =
        symbolFinder.findTopLevelPropertyGetter(COROUTINE_INTRINSICS_PACKAGE_FQNAME, COROUTINE_SUSPENDED_NAME.asString())

    val coroutineImplLabelPropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("state")!!.owner }
    val coroutineImplLabelPropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("state")!!.owner }
    val coroutineImplResultSymbolGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("result")!!.owner }
    val coroutineImplResultSymbolSetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("result")!!.owner }
    val coroutineImplExceptionPropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("exception")!!.owner }
    val coroutineImplExceptionPropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("exception")!!.owner }
    val coroutineImplExceptionStatePropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("exceptionState")!!.owner }
    val coroutineImplExceptionStatePropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("exceptionState")!!.owner }

    companion object {
        private val INTRINSICS_PACKAGE_NAME = Name.identifier("intrinsics")
        private val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")
        private val COROUTINE_IMPL_NAME = Name.identifier("CoroutineImpl")
        private val CONTINUATION_NAME = Name.identifier("Continuation")
        private val COROUTINE_PACKAGE_FQNAME = FqName.fromSegments(listOf("kotlin", "coroutines"))
        private val COROUTINE_INTRINSICS_PACKAGE_FQNAME = COROUTINE_PACKAGE_FQNAME.child(INTRINSICS_PACKAGE_NAME)
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class, InternalSymbolFinderAPI::class)
class JsSymbols(
    irBuiltIns: IrBuiltIns,
    private val stageController: StageController,
    private val intrinsics: JsIntrinsics,
) : PreSerializationJsSymbols by PreSerializationJsSymbols.Impl(irBuiltIns), JsCommonSymbols(irBuiltIns) {
    override val throwNullPointerException =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_NPE")

    init {
        symbolFinder.topLevelFunction(kotlinPackageFqn, "noWhenBranchMatchedException")
    }

    override val throwTypeCastException =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_CCE")

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "throwKotlinNothingValueException")

    override val throwISE: IrSimpleFunctionSymbol =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_ISE")

    override val throwIAE: IrSimpleFunctionSymbol =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "THROW_IAE")

    override val stringBuilder
        get() = TODO("not implemented")

    private val _arraysContentEquals = symbolFinder.topLevelFunctions(COLLECTIONS_PACKAGE_FQ_NAME, "contentEquals").filter {
        it.descriptor.extensionReceiverParameter?.type?.isMarkedNullable == true
    }

    // Can't use .owner until ExternalStubGenerator is invoked, hence get() = here.
    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
        get() = _arraysContentEquals.associateBy { it.owner.parameters[0].type.makeNotNull() }

    override val getContinuation = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, "getContinuation")

    override val returnIfSuspended = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, "returnIfSuspended")

    override val functionAdapter = symbolFinder.topLevelClass(BASE_JS_PACKAGE, "FunctionAdapter")

    override val defaultConstructorMarker = symbolFinder.topLevelClass(BASE_JS_PACKAGE, "DefaultConstructorMarker")

    override fun functionN(n: Int): IrClassSymbol {
        return stageController.withInitialIr { super.functionN(n) }
    }

    override fun suspendFunctionN(n: Int): IrClassSymbol {
        return stageController.withInitialIr { super.suspendFunctionN(n) }
    }

    internal val subStringFunction: IrSimpleFunctionSymbol =
        symbolFinder.topLevelFunction(TEXT_PACKAGE_FQ_NAME, "substring") {
            // FIXME(KT-74791): For some reason when compiling against the minimal stdlib, the symbols are unbound, but when compiling
            //   against the full stdlib, they are bound but don't have `signature`, so we have to have this if.
            if (it.isBound) {
                it.owner.hasShape(
                    extensionReceiver = true,
                    regularParameters = 2,
                    parameterTypes = listOf(irBuiltIns.stringType, irBuiltIns.intType, irBuiltIns.intType)
                )
            } else {
                (it.signature as? IdSignature.CommonSignature)?.description == "substring@kotlin.String(kotlin.Int;kotlin.Int){}"
            }
        }

    override fun isSideEffectFree(call: IrCall): Boolean =
        call.symbol in intrinsics.primitiveToLiteralConstructor.values ||
                call.symbol == intrinsics.arrayLiteral ||
                call.symbol == intrinsics.arrayConcat ||
                call.symbol == intrinsics.jsBoxIntrinsic ||
                call.symbol == intrinsics.jsUnboxIntrinsic
}
