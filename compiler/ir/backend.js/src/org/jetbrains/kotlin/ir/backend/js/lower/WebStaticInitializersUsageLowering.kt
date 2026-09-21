/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irComposite
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isEnumEntry
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Inserts calls to a static initializers function (static_init) into relevant function bodies.
 *
 * Before:
 * ```kotlin
 * class Foo {
 *   companion {
 *     var static_init_state = 1
 *     static_init() {
 *       if (!static_init_state) return
 *       if (static_init_state == 2) {
 *         staticInitializationFailureWithClassName(Foo::class)
 *       }
 *       static_init_state = 0
 *       try {
 *         first = initFirst()
 *         second = initSecond()
 *         third = initThird()
 *       } catch (reason: Throwable) {
 *         static_init_state = 2
 *         kotlin.internal.staticInitializationFailure(reason, null)
 *       }
 *     }
 *   }
 *   companion {
 *     val first: FirstType
 *   }
 *   companion object {
 *     val second: SecondType
 *   }
 *   companion {
 *     val third: ThirdType
 *   }
 * }
 * ```
 *
 * After:
 * ```kotlin
 * class Foo {
 *   constructor() {
 *     static_init()
 *   }
 *   companion {
 *     var static_init_state = 1
 *     static_init() {
 *       if (!static_init_state) return
 *       ...
 *     }
 *   }
 *   companion {
 *     val first: FirstType
 *   }
 *   companion object {
 *     val second: SecondType
 *     init {
 *       static_init()
 *     }
 *   }
 *   companion {
 *     val third: ThirdType
 *   }
 * }
 * ```
 *
 * @param initializeContainerOfInnerObject When true, access to a nested object inside a class with static initializers will cause
 *  the static_init function of that class to execute. When false, only companion object access would trigger static_init execution.
 */
abstract class WebStaticInitializersUsageLowering(
    private val context: JsCommonBackendContext,
    private val initializeContainerOfInnerObject: Boolean
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : IrTransformer<IrClass?>() {
            override fun visitClass(declaration: IrClass, data: IrClass?): IrStatement {
                insertStaticInitCall(declaration)
                return super.visitClass(declaration, declaration)
            }

            /**
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
             * So we need to prepend such direct field access with `static_init` calls.
             *
             * See KT-89290.
             */
            override fun visitGetField(expression: IrGetField, data: IrClass?): IrExpression {
                super.visitGetField(expression, data)

                val field = expression.symbol.owner
                if (!field.isStatic) return expression

                val property = field.correspondingPropertySymbol?.owner ?: return expression
                if (!property.isLateinit) return expression

                val parent = field.parent as? IrClass ?: return expression
                val staticInitFunction = parent.staticInitFunction ?: return expression

                if (data?.staticInitFunction == staticInitFunction) return expression

                return context.irBuiltIns.createIrBuilder((data ?: irFile).symbol, expression.startOffset, expression.endOffset).run {
                    irComposite(expression) {
                        +irCall(staticInitFunction.symbol)
                        +expression
                    }
                }
            }
        }, null)
    }

    private fun insertStaticInitCall(container: IrClass) {
        if (container.isEffectivelyExternal()) return
        if (container.isCompanion) return
        val staticInitFunction = container.staticInitFunction ?: return

        val builder = context.irBuiltIns.createIrBuilder(container.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        for (declaration in container.declarations) {
            when (declaration) {
                // Do not insert call to a static_init into static_init itself
                is IrSimpleFunction if declaration == staticInitFunction -> continue
                // Do not insert a call to a static_init into an enum constructor, since it would be only accessible from static_init.
                // Redundant re-entrance into static_init pollutes stepping.
                is IrConstructor if (container.isEnumClass || container.isEnumEntry) -> continue
                is IrFunction -> {
                    if (declaration.dispatchReceiverParameter != null) continue // already initialized when instance was created
                    builder.insertCall(declaration, staticInitFunction)
                }
                // If initializeObjectEnumParent is false, only call static_init from getInstance coming from the companion object.
                // JVM-based behavior, also relevant for Wasm.
                is IrClass if declaration.isObject -> {
                    val getInstance = declaration.objectGetInstanceFunction ?: continue

                    // If initializeObjectEnumParent is true, call static_init from all objects getInstance
                    // including nested objects. This behavior is K/JS-only and differs from JVM. Kept for compatibility.
                    // Please see KT-83337.
                    if (declaration.isCompanion || initializeContainerOfInnerObject) {
                        builder.insertCall(getInstance, staticInitFunction)
                    }
                }
            }
        }
    }

    private fun DeclarationIrBuilder.insertCall(target: IrFunction, staticInitFunction: IrSimpleFunction) {
        val body = target.body as? IrBlockBody ?: return
        body.statements.add(0, irCall(staticInitFunction.symbol))
    }
}
