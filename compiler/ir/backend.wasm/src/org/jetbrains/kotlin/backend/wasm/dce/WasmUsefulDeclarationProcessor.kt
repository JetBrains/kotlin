/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dce

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.ir.backend.js.dce.UsefulDeclarationProcessor
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal class WasmUsefulDeclarationProcessor(
    override val context: WasmBackendContext,
    printReachabilityInfo: Boolean,
    dumpReachabilityInfoToFile: String?
) : UsefulDeclarationProcessor(printReachabilityInfo, removeUnusedAssociatedObjects = false, dumpReachabilityInfoToFile) {

    // The mapping from function for wrapping a kotlin closure/lambda with JS closure to function used to call a kotlin closure from JS side.
    private val kotlinClosureToJsClosureConvertFunToKotlinClosureCallFun = context.fileContexts.mapValues { (_, fileContext) ->
        fileContext.kotlinClosureToJsConverters.entries.associate { (k, v) -> v to fileContext.closureCallExports[k] }
    }

    override val bodyVisitor: BodyVisitorBase = object : BodyVisitorBase() {
        override fun visitConst(expression: IrConst, data: IrDeclaration) = when (expression.kind) {
            is IrConstKind.Null -> expression.type.enqueueType(data, "expression type")
            is IrConstKind.String -> context.wasmSymbols.stringGetLiteral.owner
                .enqueue(data, "String literal intrinsic getter stringGetLiteral")
            else -> Unit
        }

        override fun visitVariable(declaration: IrVariable, data: IrDeclaration) {
            declaration.type.enqueueType(data, "local variable type")
            super.visitVariable(declaration, data)
        }

        override fun visitVararg(expression: IrVararg, data: IrDeclaration) {
            expression.type.getClass()!!
                .constructors
                .firstOrNull { it.hasWasmPrimitiveConstructorAnnotation() }
                ?.enqueue(data, "implicit vararg constructor")
            super.visitVararg(expression, data)
        }

        override fun visitSetField(expression: IrSetField, data: IrDeclaration) {
            if (!expression.symbol.owner.isObjectInstanceField()) {
                super.visitSetField(expression, data)
            }
        }

        override fun visitGetField(expression: IrGetField, data: IrDeclaration) {
            val field = expression.symbol.owner

            if (field.isObjectInstanceField()) {
                field.type.classOrFail.owner.primaryConstructor?.enqueue(field, "object lazy initialization")
            }

            super.visitGetField(expression, data)
        }

        private fun tryToProcessIntrinsicCall(from: IrDeclaration, call: IrCall): Boolean = when (call.symbol) {
            context.wasmSymbols.unboxIntrinsic -> {
                val fromType = call.getTypeArgument(0)
                if (fromType != null && !fromType.isNothing() && !fromType.isNullableNothing()) {
                    val backingField = call.getTypeArgument(1)
                        ?.let { context.inlineClassesUtils.getInlinedClass(it) }
                        ?.let { getInlineClassBackingField(it) }
                    backingField?.enqueue(from, "backing inline class field for unboxIntrinsic")
                }
                true
            }

            context.wasmSymbols.wasmTypeId,
            context.wasmSymbols.refCastNull,
            context.wasmSymbols.refTest,
            context.wasmSymbols.wasmArrayCopy -> {
                call.getTypeArgument(0)?.enqueueRuntimeClassOrAny(from, "intrinsic ${call.symbol.owner.name}")
                true
            }
            context.wasmSymbols.boxIntrinsic -> {
                val type = call.getTypeArgument(0)!!
                if (type == context.irBuiltIns.booleanType) {
                    context.wasmSymbols.getBoxedBoolean.owner.enqueue(from, "intrinsic boxIntrinsic")
                } else {
                    type.enqueueRuntimeClassOrAny(from, "intrinsic boxIntrinsic")
                }
                true
            }
            context.wasmSymbols.boxBoolean -> {
                context.irBuiltIns.booleanType.enqueueRuntimeClassOrAny(from, "intrinsic boxBoolean")
                true
            }
            else -> false
        }

        override fun visitCall(expression: IrCall, data: IrDeclaration) {
            super.visitCall(expression, data)

            val function: IrFunction = expression.symbol.owner.realOverrideTarget

            if (tryToProcessIntrinsicCall(data, expression)) return
            if (function.hasWasmNoOpCastAnnotation()) return
            if (function.getWasmOpAnnotation() != null) return

            val isSuperCall = expression.superQualifierSymbol != null
            if (function is IrSimpleFunction && function.isOverridable && !isSuperCall) {
                val klass = function.parentAsClass
                if (klass.isInterface) {
                    klass.enqueue(data, "receiver class")
                }
                function.enqueue(data, "method call")
            }
        }
    }

    private fun IrType.getInlinedValueTypeIfAny(): IrType? = when (this) {
        context.irBuiltIns.booleanType,
        context.irBuiltIns.byteType,
        context.irBuiltIns.shortType,
        context.irBuiltIns.charType,
        context.irBuiltIns.intType,
        context.irBuiltIns.longType,
        context.irBuiltIns.floatType,
        context.irBuiltIns.doubleType,
        context.irBuiltIns.nothingType,
        context.irBuiltIns.nothingNType,
        context.wasmSymbols.voidType -> null
        else -> when {
            isBuiltInWasmRefType(this) -> null
            erasedUpperBound?.isExternal == true -> null
            else -> when (val ic = context.inlineClassesUtils.getInlinedClass(this)) {
                null -> this
                else -> context.inlineClassesUtils.getInlineClassUnderlyingType(ic).getInlinedValueTypeIfAny()
            }
        }
    }

    private fun IrType.enqueueRuntimeClassOrAny(from: IrDeclaration, info: String): Unit =
        this.getRuntimeClass(context.irBuiltIns).enqueue(from, info, isContagious = false)

    private fun IrType.enqueueType(from: IrDeclaration, info: String) {
        getInlinedValueTypeIfAny()
            ?.enqueueRuntimeClassOrAny(from, info)
    }

    private fun IrDeclaration.enqueueParentClass() {
        parentClassOrNull?.enqueue(this, "parent class", isContagious = false)
    }

    override fun processField(irField: IrField) {
        super.processField(irField)
        irField.enqueueParentClass()
        irField.type.enqueueType(irField, "field types")
    }

    override fun processClass(irClass: IrClass) {
        super.processClass(irClass)

        irClass.getWasmArrayAnnotation()?.type
            ?.enqueueType(irClass, "array type for wasm array annotated")

        if (context.inlineClassesUtils.isClassInlineLike(irClass)) {
            irClass.declarations
                .firstIsInstanceOrNull<IrConstructor>()
                ?.takeIf { it.isPrimary }
                ?.enqueue(irClass, "inline class primary ctor")
        }
    }

    private fun IrValueParameter.enqueueValueParameterType(from: IrDeclaration) {
        if (context.inlineClassesUtils.shouldValueParameterBeBoxed(this)) {
            type.enqueueRuntimeClassOrAny(from, "function ValueParameterType")
        } else {
            type.enqueueType(from, "function ValueParameterType")
        }
    }

    private fun processIrFunction(irFunction: IrFunction) {
        if (irFunction.isFakeOverride) return

        val isIntrinsic = irFunction.hasWasmNoOpCastAnnotation() || irFunction.getWasmOpAnnotation() != null
        if (isIntrinsic) return

        irFunction.getEffectiveValueParameters().forEach { it.enqueueValueParameterType(irFunction) }
        irFunction.returnType.enqueueType(irFunction, "function return type")

        kotlinClosureToJsClosureConvertFunToKotlinClosureCallFun[irFunction.fileOrNull]?.get(irFunction)?.enqueue(
            irFunction,
            "kotlin closure to JS closure conversion",
            false
        )
    }

    override fun processSimpleFunction(irFunction: IrSimpleFunction) {
        super.processSimpleFunction(irFunction)
        irFunction.enqueueParentClass()
        processIrFunction(irFunction)
    }

    override fun processConstructor(irConstructor: IrConstructor) {
        super.processConstructor(irConstructor)
        val constructedClass = irConstructor.constructedClass
        if (!context.inlineClassesUtils.isClassInlineLike(constructedClass)) {
            processIrFunction(irConstructor)
        }

        // Primitive constructors has no body, since that such constructors implicitly initialize all fields, so we have to preserve them
        if (irConstructor.hasWasmPrimitiveConstructorAnnotation()) {
            constructedClass.declarations.forEach { declaration ->
                if (declaration is IrField) {
                    declaration.enqueue(constructedClass, "preserve all fields for primitive constructors")
                }
            }
        }
    }

    override fun isExported(declaration: IrDeclaration): Boolean = (declaration is IrFunction && declaration.isExported())
}
