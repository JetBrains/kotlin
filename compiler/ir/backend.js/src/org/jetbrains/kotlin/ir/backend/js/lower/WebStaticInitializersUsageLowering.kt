/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isEnumEntry
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

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
 */
abstract class WebStaticInitializersUsageLowering(private val context: JsCommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitFile(declaration: IrFile) {
                declaration.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                insertStaticInitCall(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })
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
                // Same goes for object constructors.
                is IrConstructor if (container.isEnumClass || container.isEnumEntry || container.isObject) -> continue
                is IrFunction -> {
                    if (declaration.dispatchReceiverParameter != null) continue // already initialized when instance was created
                    builder.insertCall(declaration, staticInitFunction)
                }
                is IrClass if declaration.isCompanion -> {
                    // Accessing companion objects should trigger the initialization of the containing class.
                    // This is not true for regular nested objects, they are independent of their containing class.
                    val getInstance = declaration.objectGetInstanceFunction ?: continue
                    builder.insertCall(getInstance, staticInitFunction)
                }
            }
        }
    }

    private fun DeclarationIrBuilder.insertCall(target: IrFunction, staticInitFunction: IrSimpleFunction) {
        val body = target.body as? IrBlockBody ?: return
        body.statements.add(0, irCall(staticInitFunction.symbol))
    }
}
