/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.backend.common.ir.returnType
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmSymbols
import org.jetbrains.kotlin.backend.wasm.codegen.interfaces.LoopLabelType
import org.jetbrains.kotlin.backend.wasm.codegen.interfaces.WasmFunctionCodegenContext
import org.jetbrains.kotlin.backend.wasm.lower.wasmSignature
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
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

    override fun <T> visitConst(expression: IrConst<T>) {
        when (val kind = expression.kind) {
            is IrConstKind.Null -> body.buildRefNull()
            is IrConstKind.Boolean -> body.buildConstI32(if (kind.valueOf(expression)) 1 else 0)
            is IrConstKind.Byte -> body.buildConstI32(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> body.buildConstI32(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> body.buildConstI32(kind.valueOf(expression))
            is IrConstKind.Long -> body.buildConstI64(kind.valueOf(expression))
            is IrConstKind.Char -> body.buildConstI32(kind.valueOf(expression).toInt())
            is IrConstKind.Float -> body.buildConstF32(kind.valueOf(expression))
            is IrConstKind.Double -> body.buildConstF64(kind.valueOf(expression))
            is IrConstKind.String -> {
                body.buildConstI32Symbol(
                    context.referenceStringLiteral(kind.valueOf(expression))
                )
                body.buildCall(
                    symbol = context.referenceFunction(wasmSymbols.stringGetLiteral),
                    type = WasmAnyRef
                )
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
            body.buildGetGlobal(
                global = context.referenceGlobal(field.symbol),
                type = context.transformType(field.type)
            )
        }
    }

    private fun generateInstanceFieldAccess(field: IrField) {
        body.buildStructGet(
            context.referenceStructType(field.parentAsClass.symbol),
            context.getStructFieldRef(field),
            context.transformType(field.type)
        )
    }

    override fun visitSetField(expression: IrSetField) {
        val field = expression.symbol.owner
        val receiver = expression.receiver

        if (receiver != null) {
            generateExpression(receiver)
            generateExpression(expression.value)
            body.buildStructSet(
                structType = context.referenceStructType(field.parentAsClass.symbol),
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

    override fun visitSetVariable(expression: IrSetVariable) {
        generateExpression(expression.value)
        body.buildSetLocal(context.referenceLocal(expression.symbol))
    }

    override fun visitCall(expression: IrCall) {
        generateCall(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        val klass: IrClass = expression.symbol.owner.parentAsClass

        if (backendContext.inlineClassesUtils.isClassInlineLike(klass)) {
            // Unboxed instance is just a constructor argument.
            generateExpression(expression.getValueArgument(0)!!)
            return
        }

        val wasmStructType: WasmSymbol<WasmStructType> = context.referenceStructType(klass.symbol)
        val wasmClassId = context.referenceClassId(klass.symbol)

        val irFields: List<IrField> = klass.allFields(backendContext.irBuiltIns)

        assert(irFields.isNotEmpty()) { "Class should have at least a single classId filed" }

        irFields.forEachIndexed { index, field ->
            if (index == 0)
                body.buildConstI32Symbol(wasmClassId)
            else
                generateDefaultInitializerForType(context.transformType(field.type), body)
        }

        body.buildStructNew(wasmStructType)
        generateCall(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        val klass = context.irFunction.parentAsClass

        // Don't delegate constructors of Any to Any.
        if (klass.defaultType.isAny()) {
            return
        }

        body.buildGetLocal(context.referenceLocal(0))
        generateCall(expression)
    }

    private fun generateCall(call: IrFunctionAccessExpression) {
        val function: IrFunction = call.symbol.owner.realOverrideTarget

        // Box intrinsic has an additional klass ID argument.
        // Processing it separately
        if (function.symbol == wasmSymbols.boxIntrinsic) {
            val toType = call.getTypeArgument(0)!!
            val klass = toType.erasedUpperBound!!
            val structTypeName = context.referenceStructType(klass.symbol)
            val klassId = context.referenceClassId(klass.symbol)

            body.buildConstI32Symbol(klassId)
            generateExpression(call.getValueArgument(0)!!)
            body.buildStructNew(structTypeName)
            return
        }

        call.dispatchReceiver?.let { generateExpression(it) }
        call.extensionReceiver?.let { generateExpression(it) }
        for (i in 0 until call.valueArgumentsCount) {
            generateExpression(call.getValueArgument(i)!!)
        }

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
                generateExpression(call.dispatchReceiver!!)
                body.buildConstI32(vfSlot)
                body.buildCall(context.referenceFunction(wasmSymbols.getVirtualMethodId), type = WasmI32)
            } else {
                val signatureId = context.referenceSignatureId(function.wasmSignature(backendContext.irBuiltIns))
                generateExpression(call.dispatchReceiver!!)
                body.buildConstI32Symbol(signatureId)
                body.buildCall(context.referenceFunction(wasmSymbols.getInterfaceMethodId), type = WasmI32)
            }

            body.buildCallIndirect(
                symbol = context.referenceFunctionType(function.symbol),
                type = context.transformResultType(function.returnType)
            )
        } else {  // Static function call
            val name = context.referenceFunction(function.symbol)
            body.buildCall(name, context.transformResultType(function.returnType))
        }
    }

    // Return true if generated.
    private fun tryToGenerateIntrinsicCall(
        call: IrFunctionAccessExpression,
        function: IrFunction
    ): Boolean {
        if (tryToGenerateWasmOpIntrinsicCall(function)) {
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

            wasmSymbols.structNarrow -> {
                val fromType = call.getTypeArgument(0)!!
                val toType = call.getTypeArgument(1)!!
                body.buildStructNarrow(context.transformType(fromType), context.transformType(toType))
            }

            wasmSymbols.wasmUnreachable -> {
                body.buildUnreachable()
            }

            wasmSymbols.wasmFloatNaN -> {
                body.buildF32NaN()
            }
            wasmSymbols.wasmDoubleNaN -> {
                body.buildF64NaN()
            }

            wasmSymbols.unboxIntrinsic -> {
                val fromType = call.getTypeArgument(0)!!
                val toType = call.getTypeArgument(1)!!
                val klass: IrClass = backendContext.inlineClassesUtils.getInlinedClass(toType)!!
                val field = getInlineClassBackingField(klass)

                body.buildStructNarrow(context.transformType(fromType), context.transformBoxedType(toType))
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
            body.buildRefNull()
        }

        body.buildReturn()
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
        body.buildUnary(WasmUnaryOp.I32_EQZ)
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
            val init = statement.initializer ?: return body.buildNop() // TODO: Don't nop
            generateExpression(init)
            val varName = context.referenceLocal(statement.symbol)
            body.buildSetLocal(varName)
            return
        }

        generateExpression(statement as IrExpression)

        if (statement.type != irBuiltIns.unitType && statement.type != irBuiltIns.nothingType)
            body.buildDrop()
    }

    // Return true if generated.
    fun tryToGenerateWasmOpIntrinsicCall(function: IrFunction): Boolean {
        if (function.hasWasmReinterpretAnnotation()) {
            return true
        }

        val unaryOp = function.getWasmUnaryOpAnnotation()
        if (unaryOp != null) {
            body.buildUnary(WasmUnaryOp.valueOf(unaryOp))
            return true
        }

        val binaryOp = function.getWasmBinaryOpAnnotation()
        if (binaryOp != null) {
            body.buildBinary(WasmBinaryOp.valueOf(binaryOp))
            return true
        }

        val loadOp = function.getWasmLoadOpAnnotation()
        if (loadOp != null) {
            body.buildLoad(WasmLoadOp.valueOf(loadOp), WasmMemoryArgument(0, 0))
            return true
        }

        val refOp = function.getWasmRefOpAnnotation()
        if (refOp != null) {
            when (WasmRefOp.valueOf(refOp)) {
                WasmRefOp.REF_NULL -> body.buildRefNull()
                WasmRefOp.REF_IS_NULL -> body.buildRefIsNull()
                WasmRefOp.REF_EQ -> body.buildRefEq()
            }
            return true
        }

        return false
    }
}

