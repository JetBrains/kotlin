/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.backend.common.ir.returnType
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmSymbols
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getInlineClassBackingField
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.wasm.ir.*

class BodyGenerator(val context: WasmFunctionCodegenContext) : IrElementVisitorVoid {
    val body: WasmExpressionBuilder = context.bodyGen

    // Shortcuts
    private val backendContext: WasmBackendContext = context.backendContext
    private val wasmSymbols: WasmSymbols = backendContext.wasmSymbols
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns

    override fun visitElement(element: IrElement) {
        error("Unexpected element of type ${element::class}")
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        require(expression.operator == IrTypeOperator.REINTERPRET_CAST) { "Other types of casts must be lowered" }
        generateExpression(expression.argument)
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        when (val kind = expression.kind) {
            is IrConstKind.Null -> generateDefaultInitializerForType(context.transformType(expression.type), body)
            is IrConstKind.Boolean -> body.buildConstI32(if (kind.valueOf(expression)) 1 else 0)
            is IrConstKind.Byte -> body.buildConstI32(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> body.buildConstI32(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> body.buildConstI32(kind.valueOf(expression))
            is IrConstKind.Long -> body.buildConstI64(kind.valueOf(expression))
            is IrConstKind.Char -> body.buildConstI32(kind.valueOf(expression).toInt())
            is IrConstKind.Float -> body.buildConstF32(kind.valueOf(expression))
            is IrConstKind.Double -> body.buildConstF64(kind.valueOf(expression))
            is IrConstKind.String -> {
                body.buildConstI32Symbol(context.referenceStringLiteral(kind.valueOf(expression)))
                body.buildCall(context.referenceFunction(wasmSymbols.stringGetLiteral))
            }
            else -> error("Unknown constant kind")
        }
    }

    override fun visitGetField(expression: IrGetField) {
        val field: IrField = expression.symbol.owner
        val receiver: IrExpression? = expression.receiver
        if (receiver != null) {
            generateExpression(receiver)
            if (backendContext.inlineClassesUtils.isClassInlineLike(field.parentAsClass)) {
                // Unboxed inline class instance is already represented as backing field.
                // Doing nothing.
            } else {
                generateInstanceFieldAccess(field)
            }
        } else {
            body.buildGetGlobal(context.referenceGlobal(field.symbol))
        }
    }

    private fun generateInstanceFieldAccess(field: IrField) {
        val opcode = when (field.type) {
            irBuiltIns.charType ->
                WasmOp.STRUCT_GET_U

            irBuiltIns.booleanType,
            irBuiltIns.byteType,
            irBuiltIns.shortType ->
                WasmOp.STRUCT_GET_S

            else -> WasmOp.STRUCT_GET
        }

        body.buildInstr(
            opcode,
            WasmImmediate.GcType(context.referenceGcType(field.parentAsClass.symbol)),
            WasmImmediate.StructFieldIdx(context.getStructFieldRef(field))
        )
    }

    override fun visitSetField(expression: IrSetField) {
        val field = expression.symbol.owner
        val receiver = expression.receiver

        if (receiver != null) {
            generateExpression(receiver)
            generateExpression(expression.value)
            body.buildStructSet(
                struct = context.referenceGcType(field.parentAsClass.symbol),
                fieldId = context.getStructFieldRef(field),
            )
        } else {
            generateExpression(expression.value)
            body.buildSetGlobal(context.referenceGlobal(expression.symbol))
        }
    }

    override fun visitGetValue(expression: IrGetValue) {
        body.buildGetLocal(context.referenceLocal(expression.symbol))
    }

    override fun visitSetValue(expression: IrSetValue) {
        generateExpression(expression.value)
        body.buildSetLocal(context.referenceLocal(expression.symbol))
    }

    override fun visitCall(expression: IrCall) {
        generateCall(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        val klass: IrClass = expression.symbol.owner.parentAsClass

        require(!backendContext.inlineClassesUtils.isClassInlineLike(klass)) {
            "All inline class constructor calls must be lowered to static function calls"
        }

        val wasmGcType: WasmSymbol<WasmTypeDeclaration> = context.referenceGcType(klass.symbol)

        if (klass.getWasmArrayAnnotation() != null) {
            require(expression.valueArgumentsCount == 1) { "@WasmArrayOf constructs must have exactly one argument" }
            generateExpression(expression.getValueArgument(0)!!)
            body.buildRttCanon(context.transformType(klass.defaultType))
            body.buildInstr(
                WasmOp.ARRAY_NEW_DEFAULT_WITH_RTT,
                WasmImmediate.GcType(wasmGcType)
            )
            return
        }

        val wasmClassId = context.referenceClassId(klass.symbol)

        val irFields: List<IrField> = klass.allFields(backendContext.irBuiltIns)

        irFields.forEachIndexed { index, field ->
            if (index == 0)
                body.buildConstI32Symbol(wasmClassId)
            else
                generateDefaultInitializerForType(context.transformType(field.type), body)
        }

        body.buildGetGlobal(context.referenceClassRTT(klass.symbol))
        body.buildStructNew(wasmGcType)
        generateCall(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        val klass = context.irFunction.parentAsClass

        // Don't delegate constructors of Any to Any.
        if (klass.defaultType.isAny()) {
            return
        }

        body.buildGetLocal(context.referenceLocal(0))  // this parameter
        generateCall(expression)
    }

    private fun generateCall(call: IrFunctionAccessExpression) {
        // Box intrinsic has an additional klass ID argument.
        // Processing it separately
        if (call.symbol == wasmSymbols.boxIntrinsic) {
            val toType = call.getTypeArgument(0)!!
            val klass = toType.erasedUpperBound!!
            val structTypeName = context.referenceGcType(klass.symbol)
            val klassId = context.referenceClassId(klass.symbol)

            body.buildConstI32Symbol(klassId)
            generateExpression(call.getValueArgument(0)!!)
            body.buildGetGlobal(context.referenceClassRTT(klass.symbol))
            body.buildStructNew(structTypeName)
            return
        }

        call.dispatchReceiver?.let { generateExpression(it) }
        call.extensionReceiver?.let { generateExpression(it) }
        for (i in 0 until call.valueArgumentsCount) {
            generateExpression(call.getValueArgument(i)!!)
        }

        val function: IrFunction = call.symbol.owner.realOverrideTarget

        if (tryToGenerateIntrinsicCall(call, function)) {
            return
        }

        val isSuperCall = call is IrCall && call.superQualifierSymbol != null
        if (function is IrSimpleFunction && function.isOverridable && !isSuperCall) {
            // Generating index for indirect call
            val klass = function.parentAsClass
            if (!klass.isInterface) {
                val classMetadata = context.getClassMetadata(klass.symbol)
                val vfSlot = classMetadata.virtualMethods.map { it.function }.indexOf(function)
                // Dispatch receiver should be simple and without side effects at this point
                // TODO: Verify
                generateExpression(call.dispatchReceiver!!)
                body.buildConstI32(vfSlot)
                body.buildCall(context.referenceFunction(wasmSymbols.getVirtualMethodId))
                body.buildCallIndirect(
                    symbol = context.referenceFunctionType(function.symbol)
                )
            } else {
                generateExpression(call.dispatchReceiver!!)
                body.buildConstI32Symbol(context.referenceInterfaceId(klass.symbol))
                body.buildCall(context.referenceFunction(wasmSymbols.getInterfaceImplId))
                body.buildCallIndirect(
                    tableIdx = WasmSymbolIntWrapper(context.referenceInterfaceTable(function.symbol)),
                    symbol = context.referenceFunctionType(function.symbol)
                )
            }

        } else {
            // Static function call
            body.buildCall(context.referenceFunction(function.symbol))
        }

        // Return types of imported functions cannot have concrete struct/array references.
        // Non-primitive return types are represented as eqref which need to be casted back to expected type on call site.
        if (function.getWasmImportAnnotation() != null || function.getJsFunAnnotation() != null) {
            val resT = context.transformResultType(function.returnType)
            if (resT is WasmRefNullType) {
                generateTypeRTT(function.returnType)
                body.buildRefCast(fromType = WasmEqRef, toType = resT)
            }
        }
    }

    private fun generateTypeRTT(type: IrType) {
        val rtClass = type.erasedUpperBound?.symbol ?: context.backendContext.irBuiltIns.anyClass
        body.buildGetGlobal(context.referenceClassRTT(rtClass))
    }

    // Return true if generated.
    // Assumes call arguments are already on the stack
    private fun tryToGenerateIntrinsicCall(
        call: IrFunctionAccessExpression,
        function: IrFunction
    ): Boolean {
        if (tryToGenerateWasmOpIntrinsicCall(call, function)) {
            return true
        }

        when (function.symbol) {
            wasmSymbols.wasmClassId -> {
                val klass = call.getTypeArgument(0)!!.getClass()
                    ?: error("No class given for wasmClassId intrinsic")
                assert(!klass.isInterface)
                body.buildConstI32Symbol(context.referenceClassId(klass.symbol))
            }

            wasmSymbols.wasmInterfaceId -> {
                val irInterface = call.getTypeArgument(0)!!.getClass()
                    ?: error("No interface given for wasmInterfaceId intrinsic")
                assert(irInterface.isInterface)
                body.buildConstI32Symbol(context.referenceInterfaceId(irInterface.symbol))
            }

            wasmSymbols.wasmRefCast -> {
                val fromType = call.getTypeArgument(0)!!
                val toType = call.getTypeArgument(1)!!
                generateTypeRTT(toType)
                body.buildRefCast(context.transformType(fromType), context.transformType(toType))
            }

            wasmSymbols.wasmFloatNaN -> {
                body.buildConstF32(Float.NaN)
            }
            wasmSymbols.wasmDoubleNaN -> {
                body.buildConstF64(Double.NaN)
            }

            wasmSymbols.unboxIntrinsic -> {
                val fromType = call.getTypeArgument(0)!!

                if (fromType.isNothing()) {
                    body.buildUnreachable()
                    // TODO: Investigate why?
                    return true
                }

                // Workaround test codegen/box/elvis/nullNullOk.kt
                if (fromType.makeNotNull().isNothing()) {
                    body.buildUnreachable()
                    return true
                }

                val toType = call.getTypeArgument(1)!!
                val klass: IrClass = backendContext.inlineClassesUtils.getInlinedClass(toType)!!
                val field = getInlineClassBackingField(klass)

                generateTypeRTT(toType)
                body.buildRefCast(context.transformType(fromType), context.transformBoxedType(toType))
                generateInstanceFieldAccess(field)
            }
            else -> {
                return false
            }
        }
        return true
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        val statements = expression.statements
        if (statements.isEmpty()) return

        statements.dropLast(1).forEach {
            statementToWasmInstruction(it)
        }

        if (expression.type != irBuiltIns.unitType) {
            generateExpression(statements.last() as IrExpression)
        } else {
            statementToWasmInstruction(statements.last())
        }
    }

    override fun visitBreak(jump: IrBreak) {
        body.buildBr(context.referenceLoopLevel(jump.loop, LoopLabelType.BREAK))
    }

    override fun visitContinue(jump: IrContinue) {
        body.buildBr(context.referenceLoopLevel(jump.loop, LoopLabelType.CONTINUE))
    }

    override fun visitReturn(expression: IrReturn) {
        generateExpression(expression.value)

        // FIXME: Hack for "returning" Unit from functions with generic return type.
        //        Common case -- lambdas returning unit.
        if (expression.value.type == irBuiltIns.unitType &&
            expression.returnTargetSymbol.owner.returnType(backendContext) != irBuiltIns.unitType
        ) {
            val irReturnType = expression.returnTargetSymbol.owner.returnType(backendContext)

            if (irReturnType != irBuiltIns.unitType) {
                generateDefaultInitializerForType(context.transformType(irReturnType), body)
            }
        }

        body.buildInstr(WasmOp.RETURN)
    }

    override fun visitWhen(expression: IrWhen) {
        if (expression.type == irBuiltIns.unitType) {
            var ifCount = 0
            for (branch in expression.branches) {
                if (!isElseBranch(branch)) {
                    generateExpression(branch.condition)
                    body.buildIf(label = null, resultType = null)
                    statementToWasmInstruction(branch.result)
                    body.buildElse()
                    ifCount++
                } else {
                    statementToWasmInstruction(branch.result)
                    break
                }
            }

            repeat(ifCount) { body.buildEnd() }
            return
        }

        val resultType = context.transformBlockResultType(expression.type)
        var ifCount = 0
        for (branch in expression.branches) {
            if (!isElseBranch(branch)) {
                generateExpression(branch.condition)
                body.buildIf(null, resultType)
                generateExpression(branch.result)
                if (expression.type == irBuiltIns.nothingType) {
                    body.buildUnreachable()
                }
                body.buildElse()
                ifCount++
            } else {
                generateExpression(branch.result)
                if (expression.type == irBuiltIns.nothingType) {
                    body.buildUnreachable()
                }
                break
            }
        }

        repeat(ifCount) { body.buildEnd() }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        // (loop $LABEL
        //     (block $BREAK_LABEL
        //         (block $CONTINUE_LABEL <LOOP BODY>)
        //         (br_if $LABEL          <CONDITION>)))

        val label = loop.label

        body.buildLoop(label)
        val wasmLoop = body.numberOfNestedBlocks

        body.buildBlock("BREAK_$label")
        val wasmBreakBlock = body.numberOfNestedBlocks

        body.buildBlock("CONTINUE_$label")
        val wasmContinueBlock = body.numberOfNestedBlocks

        context.defineLoopLevel(loop, LoopLabelType.BREAK, wasmBreakBlock)
        context.defineLoopLevel(loop, LoopLabelType.CONTINUE, wasmContinueBlock)

        loop.body?.let { statementToWasmInstruction(it) }
        body.buildEnd()
        generateExpression(loop.condition)
        body.buildBrIf(wasmLoop)
        body.buildEnd()
        body.buildEnd()
    }

    override fun visitWhileLoop(loop: IrWhileLoop) {
        // (loop $CONTINUE_LABEL
        //     (block $BREAK_LABEL
        //         (br_if $BREAK_LABEL (i32.eqz <CONDITION>))
        //         <LOOP_BODY>
        //         (br $CONTINUE_LABEL)))

        val label = loop.label

        body.buildLoop(label)
        val wasmLoop = body.numberOfNestedBlocks

        body.buildBlock("BREAK_$label")
        val wasmBreakBlock = body.numberOfNestedBlocks

        context.defineLoopLevel(loop, LoopLabelType.BREAK, wasmBreakBlock)
        context.defineLoopLevel(loop, LoopLabelType.CONTINUE, wasmLoop)

        generateExpression(loop.condition)
        body.buildInstr(WasmOp.I32_EQZ)
        body.buildBrIf(wasmBreakBlock)
        loop.body?.let {
            statementToWasmInstruction(it)
        }
        body.buildBr(wasmLoop)
        body.buildEnd()
        body.buildEnd()
    }

    fun generateExpression(expression: IrExpression) {
        expression.acceptVoid(this)

        if (expression.type == irBuiltIns.nothingType) {
            body.buildUnreachable()
        }
    }

    fun statementToWasmInstruction(statement: IrStatement) {
        if (statement is IrVariable) {
            context.defineLocal(statement.symbol)
            val init = statement.initializer ?: return
            generateExpression(init)
            val varName = context.referenceLocal(statement.symbol)
            body.buildSetLocal(varName)
            return
        }

        generateExpression(statement as IrExpression)

        if (statement.type != irBuiltIns.unitType && statement.type != irBuiltIns.nothingType) {
            body.buildInstr(WasmOp.DROP)
        }
    }

    // Return true if function is recognized as intrinsic.
    fun tryToGenerateWasmOpIntrinsicCall(call: IrFunctionAccessExpression, function: IrFunction): Boolean {
        if (function.hasWasmReinterpretAnnotation()) {
            return true
        }

        val opString = function.getWasmOpAnnotation()
        if (opString != null) {
            val op = WasmOp.valueOf(opString)
            var immediates = emptyArray<WasmImmediate>()
            when (op.immediates.size) {
                0 -> {
                }
                1 -> {
                    immediates = arrayOf(
                        when (val imm = op.immediates[0]) {
                            WasmImmediateKind.MEM_ARG ->
                                WasmImmediate.MemArg(0u, 0u)
                            WasmImmediateKind.STRUCT_TYPE_IDX ->
                                WasmImmediate.GcType(context.referenceGcType(function.dispatchReceiverParameter!!.type.classOrNull!!))
                            else ->
                                error("Immediate $imm is unsupported")
                        }
                    )
                }
                2 -> {
                    when (op) {
                        WasmOp.REF_TEST -> {
                            val fromIrType = call.getValueArgument(0)!!.type
                            val fromWasmType = context.transformBoxedType(fromIrType)
                            val toIrType = call.getTypeArgument(0)!!
                            val toWasmType = context.transformBoxedType(toIrType)
                            immediates = arrayOf(
                                WasmImmediate.HeapType(fromWasmType),
                                WasmImmediate.HeapType(toWasmType),
                            )

                            // ref.test takes RTT as a second operand
                            generateTypeRTT(toIrType)
                        }
                        else ->
                            error("Op $opString is unsupported")
                    }
                }
                else ->
                    error("Op $opString is unsupported")
            }
            body.buildInstr(op, *immediates)
            return true
        }

        return false
    }
}

