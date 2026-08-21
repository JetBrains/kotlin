/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.jsConstructorReference
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.irCastIfNeeded
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.name.Name

abstract class LazyGlobalInitializationGenerator {
    protected abstract val backendContext: JsCommonBackendContext

    private object InitializationState {
        const val UNINITIALIZED: Int = 0
        const val INITIALIZED: Int = 1
        const val ERROR: Int = 2
    }

    protected abstract fun IrBuilderWithScope.generateStaticInitializationStateCheck(getStateField: IrGetField, klass: IrClass?): IrCall

    protected open fun IrBuilderWithScope.undefinedOrNull(): IrExpression = irNull()

    protected open val catchParameterType: IrType
        get() = backendContext.irBuiltIns.throwableType

    internal fun createStateField(name: Name, origin: IrDeclarationOrigin): IrField = backendContext.irFactory.buildField {
        this.name = name
        this.origin = origin
        type = backendContext.irBuiltIns.intType
        visibility = DescriptorVisibilities.PRIVATE
        isStatic = true
        isFinal = true
    }.apply {
        initializer = backendContext.irFactory.createExpressionBody(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            IrConstImpl.int(type = backendContext.irBuiltIns.intType, value = InitializationState.UNINITIALIZED),
        )
    }

    internal fun createStaticInitFunction(
        name: Name,
        klass: IrClass?,
        origin: IrDeclarationOrigin,
        stateField: IrField,
        initializers: List<IrStatement>,
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE,
        beforeAll: IrBlockBuilder.() -> Unit = {},
    ): IrSimpleFunction {
        val initFunction = backendContext.irFactory.buildFun {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            this.origin = origin
            this.name = name
            this.visibility = visibility
            returnType = backendContext.irBuiltIns.unitType
        }
        return initFunction.apply {
            val builder = backendContext.createIrBuilder(symbol)
            body = backendContext.irFactory.createBlockBody(startOffset, endOffset) {
                with(builder) {
                    val stateCheck = generateStaticInitializationStateCheck(irGetField(null, stateField), klass)
                    statements += irIfThen(stateCheck, irReturnUnit())
                    statements += irSetField(null, stateField, irInt(InitializationState.INITIALIZED))
                    val allInitializers = irComposite {
                        beforeAll()
                        for (initializer in initializers) {
                            initializer.setDeclarationsParent(initFunction)
                        }
                        +initializers
                    }
                    val catchParameter = scope.createTemporaryVariableDeclaration(
                        irType = catchParameterType,
                        nameHint = "reason",
                        origin = IrDeclarationOrigin.CATCH_PARAMETER,
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        inventUniqueName = false,
                    )
                    val catchResult = irComposite {
                        +irSetField(null, stateField, irInt(InitializationState.ERROR))
                        +irCall(this@LazyGlobalInitializationGenerator.backendContext.symbols.staticInitializationFailure).apply {
                            arguments[0] = irCastIfNeeded(irGet(catchParameter), context.irBuiltIns.throwableType)
                            arguments[1] = undefinedOrNull()
                        }
                    }
                    statements += irTry(context.irBuiltIns.unitType, allInitializers, listOf(irCatch(catchParameter, catchResult)), null)
                }
            }
        }
    }
}

class JsLazyGlobalInitializationGenerator(override val backendContext: JsIrBackendContext) : LazyGlobalInitializationGenerator() {
    override fun IrBuilderWithScope.generateStaticInitializationStateCheck(getStateField: IrGetField, klass: IrClass?): IrCall =
        irCall(backendContext.symbols.checkStaticInitializationState).apply {
            arguments[0] = getStateField
            arguments[1] = klass?.jsConstructorReference(backendContext) ?: backendContext.getVoid()
        }

    override fun IrBuilderWithScope.undefinedOrNull(): IrExpression = backendContext.getVoid()

    override val catchParameterType: IrType
        get() = backendContext.dynamicType
}
