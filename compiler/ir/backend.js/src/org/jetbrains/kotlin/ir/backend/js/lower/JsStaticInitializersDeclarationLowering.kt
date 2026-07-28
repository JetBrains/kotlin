/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.jsConstructorReference
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

@PhasePrerequisites(
    ObjectDeclarationLowering::class,
    EnumEntryInstancesLowering::class,
    EnumEntryCreateGetInstancesFunsLowering::class,
)
class JsStaticInitializersDeclarationLowering(override val context: JsIrBackendContext) : WebStaticInitializersDeclarationLowering() {
    private fun IrBuilderWithScope.generateStaticInitializationStateCheck(getStateField: IrGetField, container: IrClass): IrCall =
        irCall(this@JsStaticInitializersDeclarationLowering.context.symbols.checkStaticInitializationState).apply {
            arguments[0] = getStateField
            arguments[1] = container.jsConstructorReference(this@JsStaticInitializersDeclarationLowering.context)
        }

    override fun IrBuilderWithScope.undefinedOrNull(): IrExpression = this@JsStaticInitializersDeclarationLowering.context.getVoid()

    override val catchParameterType: IrType
        get() = context.dynamicType

    protected override fun createStaticInitFunction(
        container: IrClass,
        origin: IrDeclarationOrigin,
        initCalledVar: IrField,
        initializers: List<IrStatement>
    ): IrSimpleFunction {
        val initFunction = context.irFactory.buildFun {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            this.origin = origin
            name = Name.identifier(STATIC_INIT_FUNCTION_NAME)
            visibility = DescriptorVisibilities.PUBLIC
            returnType = context.irBuiltIns.unitType
        }
        return initFunction.apply {
            val builder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET)
            parent = container
            body = context.irFactory.createBlockBody(startOffset, endOffset) {
                with(builder) {
                    val stateCheck = generateStaticInitializationStateCheck(irGetField(null, initCalledVar), container)
                    statements += irIfThen(stateCheck, irReturnUnit())
                    initializationBody(initFunction, container, initCalledVar, initializers)
                }
            }
        }
    }
}
