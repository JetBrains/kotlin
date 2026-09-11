/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irComposite
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * A lowering that prepends `IrGetField` external (not from the same container) access for `lateinit` properties with `static_init` call.
 *
 * A `lateinit` backing field can be accessed directly, without a getter, which skips `static_init` call in some cases.
 *
 * For example, [org.jetbrains.kotlin.backend.common.lower.LateinitLowering] lowers `::prop.isInitialized` into a null-check `if`
 * of the underlying backing field, skipping the `get_prop` getter call at all:
 * ```
 * ::prop.isInitialized
 * ```
 * becomes
 * ```
 * if (prop_field != null) true else false
 * ```
 *
 * See KT-89290.
 */
abstract class WebStaticInitializersGetFieldUsageLowering(private val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetField(expression: IrGetField): IrExpression {
                expression.transformChildrenVoid(this)

                val field = expression.symbol.owner
                if (!field.isStatic) return expression

                val property = field.correspondingPropertySymbol?.owner ?: return expression
                if (!property.isLateinit) return expression

                val parent = field.parent as? IrClass ?: return expression
                val staticInitFunction = parent.staticInitFunction ?: return expression

                if (container.parentClassOrNull == parent) return expression

                return context.irBuiltIns.createIrBuilder(container.symbol, expression.startOffset, expression.endOffset).run {
                    irComposite(expression) {
                        +irCall(staticInitFunction.symbol)
                        +expression
                    }
                }
            }
        })
    }
}
