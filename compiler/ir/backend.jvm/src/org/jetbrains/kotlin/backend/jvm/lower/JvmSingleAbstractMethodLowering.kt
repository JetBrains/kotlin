/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.SingleAbstractMethodLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.load.java.JavaVisibilities

internal val singleAbstractMethodPhase = makeIrFilePhase(
    ::JvmSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes"
)

private class JvmSingleAbstractMethodLowering(context: JvmBackendContext) : SingleAbstractMethodLowering(context) {
    private val jvmContext: JvmBackendContext get() = context as JvmBackendContext

    override val privateGeneratedWrapperVisibility: Visibility
        get() = JavaVisibilities.PACKAGE_VISIBILITY

    override fun getSuperTypeForWrapper(typeOperand: IrType): IrType =
        typeOperand.erasedUpperBound.defaultType

    private val IrType.isKotlinFunInterface: Boolean
        get() = getClass()?.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

    override fun getAdditionalSupertypes(supertype: IrType): List<IrType> =
        if (supertype.isKotlinFunInterface) listOf(jvmContext.ir.symbols.functionAdapter.owner.typeWith())
        else emptyList()

    override fun generateEqualsHashCode(klass: IrClass, supertype: IrType, functionDelegateField: IrField) {
        if (!supertype.isKotlinFunInterface) return

        SamEqualsHashCodeMethodsGenerator(jvmContext, klass, supertype) { receiver ->
            irGetField(receiver, functionDelegateField)
        }.generate()
    }
}

/**
 * Generates equals and hashCode for SAM and fun interface wrappers, as well as an implementation of getFunctionDelegate
 * (inherited from kotlin.jvm.internal.FunctionAdapter), needed to properly implement them.
 * This class is used in two places:
 * - FunctionReferenceLowering, which is the case of SAM conversion of a (maybe adapted) function reference, e.g. `FunInterface(foo::bar)`.
 *   Note that we don't generate equals/hashCode for SAM conversion of lambdas, e.g. `FunInterface {}`, even though lambdas are represented
 *   as a local function + reference to it. The reason for this is that all lambdas are unique, so after SAM conversion they are still
 *   never equal to each other. See [FunctionReferenceLowering.FunctionReferenceBuilder.needToGenerateSamEqualsHashCodeMethods].
 * - JvmSingleAbstractMethodLowering, which is the case of SAM conversion of any value of a functional type,
 *   e.g. `val f = {}; FunInterface(f)`.
 */
internal class SamEqualsHashCodeMethodsGenerator(
    private val context: JvmBackendContext,
    private val klass: IrClass,
    private val samSuperType: IrType,
    private val obtainFunctionDelegate: IrBuilderWithScope.(receiver: IrExpression) -> IrExpression,
) {
    private val builtIns: IrBuiltIns get() = context.irBuiltIns
    private val functionAdapterClass = context.ir.symbols.functionAdapter.owner
    private val getFunctionDelegate = functionAdapterClass.functions.single { it.name.asString() == "getFunctionDelegate" }

    fun generate() {
        generateGetFunctionDelegate()
        generateEquals()
        generateHashCode()
    }

    private fun generateGetFunctionDelegate() {
        klass.addFunction(getFunctionDelegate.name.asString(), getFunctionDelegate.returnType).apply {
            overriddenSymbols = listOf(getFunctionDelegate.symbol)
            body = context.createIrBuilder(symbol).run {
                irExprBody(obtainFunctionDelegate(irGet(dispatchReceiverParameter!!)))
            }
        }
    }

    private fun generateEquals() {
        klass.addFunction("equals", builtIns.booleanType).apply {
            overriddenSymbols = listOf(samSuperType.getClass()!!.functions.single {
                it.name.asString() == "equals" &&
                        it.extensionReceiverParameter == null &&
                        it.valueParameters.singleOrNull()?.type == builtIns.anyNType
            }.symbol)

            val other = addValueParameter("other", builtIns.anyNType)
            body = context.createIrBuilder(symbol).run {
                irExprBody(
                    irIfThenElse(
                        builtIns.booleanType,
                        irIs(irGet(other), samSuperType),
                        irIfThenElse(
                            builtIns.booleanType,
                            irIs(irGet(other), functionAdapterClass.typeWith()),
                            irEquals(
                                irCall(getFunctionDelegate).also {
                                    it.dispatchReceiver = irGet(dispatchReceiverParameter!!)
                                },
                                irCall(getFunctionDelegate).also {
                                    it.dispatchReceiver = irImplicitCast(irGet(other), functionAdapterClass.typeWith())
                                }
                            ),
                            irFalse()
                        ),
                        irFalse()
                    )
                )
            }
        }
    }

    private fun generateHashCode() {
        klass.addFunction("hashCode", builtIns.intType).apply {
            val hashCode = klass.superTypes.first().getClass()!!.functions.single {
                it.name.asString() == "hashCode" && it.extensionReceiverParameter == null && it.valueParameters.isEmpty()
            }.symbol
            overriddenSymbols = listOf(hashCode)
            body = context.createIrBuilder(symbol).run {
                irExprBody(
                    irCall(hashCode).also {
                        it.dispatchReceiver = irCall(getFunctionDelegate).also {
                            it.dispatchReceiver = irGet(dispatchReceiverParameter!!)
                        }
                    }
                )
            }
        }
    }
}
