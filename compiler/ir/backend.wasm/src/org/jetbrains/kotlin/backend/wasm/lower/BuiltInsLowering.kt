/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.utils.erasedUpperBound
import org.jetbrains.kotlin.ir.backend.js.utils.isEqualsInheritedFromAny
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.parentOrNull

class BuiltInsLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val irBuiltins = context.irBuiltIns
    private val symbols = context.wasmSymbols

    private fun IrType.findEqualsMethod(): IrSimpleFunction {
        val klass = getClass() ?: irBuiltins.anyClass.owner
        return klass.functions.single { it.isEqualsInheritedFromAny() }
    }

    fun transformCall(
        call: IrCall,
        builder: DeclarationIrBuilder
    ): IrExpression {
        when (val symbol = call.symbol) {
            irBuiltins.ieee754equalsFunByOperandType[irBuiltins.floatClass] -> {
                if (call.getValueArgument(0)!!.type.isNullable() || call.getValueArgument(1)!!.type.isNullable()) {
                    return irCall(call, symbols.nullableFloatIeee754Equals)
                }
                return irCall(call, symbols.floatEqualityFunctions.getValue(irBuiltins.floatType))
            }
            irBuiltins.ieee754equalsFunByOperandType[irBuiltins.doubleClass] -> {
                if (call.getValueArgument(0)!!.type.isNullable() || call.getValueArgument(1)!!.type.isNullable()) {
                    return irCall(call, symbols.nullableDoubleIeee754Equals)
                }
                return irCall(call, symbols.floatEqualityFunctions.getValue(irBuiltins.doubleType))
            }
            irBuiltins.eqeqSymbol -> {
                val lhs = call.getValueArgument(0)!!
                val rhs = call.getValueArgument(1)!!
                val lhsType = lhs.type
                val rhsType = rhs.type
                if (lhsType == rhsType) {
                    val newSymbol = symbols.equalityFunctions[lhsType]
                    if (newSymbol != null) {
                        return irCall(call, newSymbol)
                    }
                }
                if (lhs.isNullConst()) {
                    val refIsNull = if (rhsType.erasedUpperBound?.isExternal == true) symbols.externRefIsNull else symbols.refIsNull
                    return builder.irCall(refIsNull).apply { putValueArgument(0, rhs) }
                }
                if (rhs.isNullConst()) {
                    val refIsNull = if (lhsType.erasedUpperBound?.isExternal == true) symbols.externRefIsNull else symbols.refIsNull
                    return builder.irCall(refIsNull).apply { putValueArgument(0, lhs) }
                }
                if (!lhsType.isNullable()) {
                    return irCall(call, lhsType.findEqualsMethod().symbol, argumentsAsReceivers = true)
                }
                return irCall(call, symbols.nullableEquals)
            }

            irBuiltins.eqeqeqSymbol -> {
                val type = call.getValueArgument(0)!!.type
                val newSymbol = symbols.equalityFunctions[type] ?: symbols.floatEqualityFunctions[type] ?: symbols.refEq
                return irCall(call, newSymbol)
            }

            irBuiltins.checkNotNullSymbol -> {
                val arg = call.getValueArgument(0)!!

                if (arg.isNullConst()) {
                    return builder.irCall(symbols.throwNullPointerException)
                }

                return builder.irComposite {
                    val temporary = irTemporary(arg)
                    +builder.irIfNull(
                        type = arg.type.makeNotNull(),
                        subject = irGet(temporary),
                        thenPart = builder.irCall(symbols.throwNullPointerException),
                        elsePart = irGet(temporary)
                    )
                }
            }
            in symbols.comparisonBuiltInsToWasmIntrinsics.keys -> {
                val newSymbol = symbols.comparisonBuiltInsToWasmIntrinsics[symbol]!!
                return irCall(call, newSymbol)
            }

            irBuiltins.noWhenBranchMatchedExceptionSymbol ->
                return builder.irCall(symbols.throwNoBranchMatchedException, irBuiltins.nothingType)

            irBuiltins.illegalArgumentExceptionSymbol ->
                return builder.irCall(symbols.throwIAE, irBuiltins.nothingType, 1).apply {
                    putValueArgument(0, call.getValueArgument(0)!!)
                }

            irBuiltins.dataClassArrayMemberHashCodeSymbol, irBuiltins.dataClassArrayMemberToStringSymbol -> {
                val argument = call.getValueArgument(0)!!
                val argumentType = argument.type
                val overloadSymbol: IrSimpleFunctionSymbol
                val returnType: IrType
                if (symbol == irBuiltins.dataClassArrayMemberHashCodeSymbol) {
                    overloadSymbol = symbols.findContentHashCodeOverload(argumentType)
                    returnType = irBuiltins.intType
                } else {
                    overloadSymbol = symbols.findContentToStringOverload(argumentType)
                    returnType = irBuiltins.stringType
                }

                return builder.irCall(
                    overloadSymbol,
                    returnType,
                ).apply {
                    extensionReceiver = argument
                    if (argumentType.classOrNull == irBuiltins.arrayClass) {
                        putTypeArgument(0, argumentType.getArrayElementType(irBuiltins))
                    }
                }
            }
            in symbols.startCoroutineUninterceptedOrReturnIntrinsics -> {
                val arity = symbols.startCoroutineUninterceptedOrReturnIntrinsics.indexOf(symbol)
                val newSymbol = irBuiltins.suspendFunctionN(arity).getSimpleFunction("invoke")!!
                return irCall(call, newSymbol, argumentsAsReceivers = true)
            }
            symbols.reflectionSymbols.getClassData -> {
                val infoDataCtor = symbols.reflectionSymbols.wasmTypeInfoData.constructors.first()
                val type = call.getTypeArgument(0)!!
                val isInterface = type.isInterface()
                val fqName = type.classFqName!!
                val fqnShouldBeEmitted =
                    context.configuration.languageVersionSettings.getFlag(AnalysisFlags.allowFullyQualifiedNameInKClass)
                val packageName = if (fqnShouldBeEmitted) fqName.parentOrNull()?.asString() ?: "" else ""
                val typeName = fqName.shortName().asString()

                return with(builder) {
                    val wasmIdGetter = if (type.isInterface()) symbols.wasmInterfaceId else symbols.wasmClassId
                    val typeId = irCall(wasmIdGetter).also {
                        it.putTypeArgument(0, type)
                    }

                    irCallConstructor(infoDataCtor, emptyList()).also {
                        it.putValueArgument(0, typeId)
                        it.putValueArgument(1, isInterface.toIrConst(context.irBuiltIns.booleanType))
                        it.putValueArgument(2, packageName.toIrConst(context.irBuiltIns.stringType))
                        it.putValueArgument(3, typeName.toIrConst(context.irBuiltIns.stringType))
                    }
                }
            }
        }

        return call
    }

    override fun lower(irFile: IrFile) {
        val builder = context.createIrBuilder(irFile.symbol)
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val newExpression = transformCall(expression, builder)
                newExpression.transformChildrenVoid(this)
                return newExpression
            }
        })
    }
}
