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
    printReachabilityInfo: Boolean
) : UsefulDeclarationProcessor(printReachabilityInfo, removeUnusedAssociatedObjects = false) {

    private val unitGetInstance: IrSimpleFunction = context.findUnitGetInstanceFunction()

    override val bodyVisitor: BodyVisitorBase = object : BodyVisitorBase() {
        override fun visitConst(expression: IrConst<*>, data: IrDeclaration) = when (expression.kind) {
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
            context.wasmSymbols.boxIntrinsic,
            context.wasmSymbols.wasmArrayCopy -> {
                call.getTypeArgument(0)?.enqueueRuntimeClassOrAny(from, "intrinsic ${call.symbol.owner.name}")
                true
            }

            else -> false
        }

        override fun visitCall(expression: IrCall, data: IrDeclaration) {
            super.visitCall(expression, data)

            val function: IrFunction = expression.symbol.owner.realOverrideTarget
            if (function.returnType == context.irBuiltIns.unitType) {
                unitGetInstance.enqueue(data, "function Unit return type")
            }

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
    }

    override fun processSimpleFunction(irFunction: IrSimpleFunction) {
        super.processSimpleFunction(irFunction)
        irFunction.enqueueParentClass()
        if (irFunction.isFakeOverride) {
            irFunction.overriddenSymbols.forEach { overridden ->
                overridden.owner.enqueue(irFunction, "original for fake-override")
            }
        }
        processIrFunction(irFunction)
    }

    override fun processConstructor(irConstructor: IrConstructor) {
        super.processConstructor(irConstructor)
        if (!context.inlineClassesUtils.isClassInlineLike(irConstructor.parentAsClass)) {
            processIrFunction(irConstructor)
        }
    }

    override fun isExported(declaration: IrDeclaration): Boolean = declaration.isJsExport()
}
