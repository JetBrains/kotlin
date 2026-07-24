/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.jsConstructorReference
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.types.IrType

@PhasePrerequisites(
    ObjectDeclarationLowering::class,
    EnumEntryInstancesLowering::class,
    EnumEntryCreateGetInstancesFunsLowering::class,
)
class JsStaticInitializersDeclarationLowering(override val context: JsIrBackendContext) : WebStaticInitializersDeclarationLowering() {
    override fun IrBuilderWithScope.generateStaticInitializationStateCheck(getStateField: IrGetField, container: IrClass): IrCall =
        irCall(this@JsStaticInitializersDeclarationLowering.context.symbols.checkStaticInitializationState).apply {
            arguments[0] = getStateField
            arguments[1] = container.jsConstructorReference(this@JsStaticInitializersDeclarationLowering.context)
        }

    override fun IrBuilderWithScope.undefinedOrNull(): IrExpression = this@JsStaticInitializersDeclarationLowering.context.getVoid()

    override val catchParameterType: IrType
        get() = context.dynamicType
}
