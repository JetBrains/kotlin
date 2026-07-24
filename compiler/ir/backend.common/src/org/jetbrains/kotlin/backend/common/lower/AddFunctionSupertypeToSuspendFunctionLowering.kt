/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.coroutines.defaultLoweredSuspendFunctionReturnType
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.getAllSubstitutedSupertypes
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.collections.plus

/**
 * This lowering is a hack to provide compatibility with leaked Kotlin/JVM implementation detail.
 *
 * On Kotlin/JVM, `SuspendFunctionN<Args, Ret>` is implicitly implementing `FunctionN+1<Args, Continuation, Any(?)>`,
 * and vice versa, and it can be used for reusing continuation objects for performance improvements.
 *
 * Also, it is used in the `startCoroutineUninterceptedOrReturn` intrinsic.
 *
 * So we are adding the corresponding `Function{N+1}` into the list of supertypes.
 * At the current point, lowered suspend function signature is overriding its invoke method.
 */
open class AddFunctionSupertypeToSuspendFunctionLowering(open val context: CommonBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass) return null
        addMissingSupertypes(declaration)
        return listOf(declaration)
    }

    protected open fun getLoweredForSuspendFun(irFunction: IrSimpleFunction): IrSimpleFunction =
        irFunction.getOrCreateFunctionWithContinuationStub(context, ::transformReturnType)

    protected open fun transformReturnType(suspendFunctionReturnType: IrType) =
        defaultLoweredSuspendFunctionReturnType(suspendFunctionReturnType, context.irBuiltIns)

    private fun IrClass.getLoweredInvokeMethod(): IrSimpleFunction {
        val invokeMethod = simpleFunctions().single {
            it.name == OperatorNameConventions.INVOKE
        }
        if (!invokeMethod.isSuspend) return invokeMethod

        return getLoweredForSuspendFun(invokeMethod)
    }

    private fun addOverride(clazz: IrClass, alreadyOverridden: IrType, toOverride: IrType) {
        val alreadyOverriddenFunction = alreadyOverridden.classOrNull!!.owner.getLoweredInvokeMethod()
        val functionToOverride = toOverride.classOrNull!!.owner.getLoweredInvokeMethod()
        val invokeFunction = clazz.simpleFunctions().single { it.overrides(alreadyOverriddenFunction) }
        if (invokeFunction.modality == Modality.ABSTRACT) return
        clazz.superTypes += toOverride
        invokeFunction.overriddenSymbols += functionToOverride.symbol
    }

    private fun IrSimpleType.getClassAt(index: Int) = (this.arguments.getOrNull(index) as? IrTypeProjection)?.type?.classOrNull

    private val continuationClassSymbol get() = context.symbols.continuationClass

    protected open fun addMissingSupertypes(clazz: IrClass) {
        val supertypes = getAllSubstitutedSupertypes(clazz)
        addFunctionSupertypesToSuspendFunctions(clazz, supertypes)
        addSuspendFunctionSupertypesToFunctions(clazz, supertypes)
    }

    /**
     * Adds `(K)FunctionN+1<P1..Pn, Continuation<R>, Any(?)>` as a supertype of each `(K)SuspendFunctionN<P1..Pn, R>` supertype.
     */
    protected fun addFunctionSupertypesToSuspendFunctions(clazz: IrClass, supertypes: Set<IrSimpleType>) {
        val suspendFunctionSuperTypes = supertypes.filter {
            // SuspendFunction class is some hack in old Kotlin/Native compiler versions.
            // It's not used now, but is considered as SuspendFunction-like class in isSuspendFunction util,
            // if found in old klib. We need just to ignore it.
            it.isSuspendFunction() && it.classOrNull?.owner?.name?.toString() != "SuspendFunction"
                    || it.isKSuspendFunction()
        }

        for (suspendFunctionType in suspendFunctionSuperTypes) {
            val functionClassTypeArguments = suspendFunctionType.arguments.mapIndexed { index, argument ->
                val type = (argument as IrTypeProjection).type
                if (index == suspendFunctionType.arguments.indices.last) {
                    continuationClassSymbol.typeWith(type)
                } else {
                    type
                }
            } + transformReturnType((suspendFunctionType.arguments.last() as IrTypeProjection).type)

            val genericFunctionSuperType =
                if (suspendFunctionType.isSuspendFunction()) {
                    context.irBuiltIns.functionN(functionClassTypeArguments.size - 1)
                } else {
                    context.irBuiltIns.kFunctionN(functionClassTypeArguments.size - 1)
                }

            val functionType = genericFunctionSuperType.typeWith(functionClassTypeArguments)

            addOverride(clazz, suspendFunctionType, functionType)
        }
    }

    /**
     * Adds the reverse `(K)SuspendFunctionN<P1..Pn, R>` supertype to each `(K)FunctionN+1<P1..Pn, Continuation<R>, Any(?)>` supertype.
     */
    protected fun addSuspendFunctionSupertypesToFunctions(clazz: IrClass, supertypes: Set<IrSimpleType>) {
        val functionWithContinuationSuperTypes = supertypes.filter {
            (it.isFunction() || it.isKFunction()) &&
                    it.getClassAt(it.arguments.size - 2) == continuationClassSymbol
        }

        for (functionType in functionWithContinuationSuperTypes) {
            val suspendFunctionClassTypeArguments = functionType.arguments.dropLast(1).mapIndexed { index, argument ->
                val type = (argument as IrTypeProjection).type
                if (index == functionType.arguments.indices.last - 1) {
                    require(type.classOrNull == continuationClassSymbol)
                    when (val typeArgument = (type as IrSimpleType).arguments.single()) {
                        is IrTypeProjection -> typeArgument.type
                        is IrStarProjection -> context.irBuiltIns.anyNType
                    }
                } else {
                    type
                }
            }

            val genericSuspendFunctionType =
                if (functionType.isFunction()) {
                    context.irBuiltIns.suspendFunctionN(suspendFunctionClassTypeArguments.size - 1)
                } else {
                    context.irBuiltIns.kSuspendFunctionN(suspendFunctionClassTypeArguments.size - 1)
                }

            val suspendFunctionType = genericSuspendFunctionType.typeWith(suspendFunctionClassTypeArguments)
            addOverride(clazz, functionType, suspendFunctionType)
        }
    }
}
