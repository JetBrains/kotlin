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
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.getInstanceFun
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isEnumEntry
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Inserts calls to a static initializers function (static_init) into relevant function bodies.
 *
 * @param initializeContainerOfInnerObject When true, access to a nested object inside a class with static initializers will cause
 *  the static_init function of that class to execute. When false, only companion object access would trigger static_init execution.
 *
 * Before:
 * ```kotlin
 * class Foo {
 *   companion {
 *     var static_init_called = false
 *     static_init() {
 *       if (static_init_called) return
 *       static_init_called = true
 *       first = initFirst()
 *       second = initSecond()
 *       third = initThird()
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
 *     var static_init_called = false
 *     static_init() {
 *       static_init_called = true
 *       // ...
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
 */
abstract class WebStaticInitializersUsageLowering(
    private val context: JsCommonBackendContext,
    private val initializeContainerOfInnerObject: Boolean
) : FileLoweringPass {
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
        val staticInitFunction = container.staticInitFunction ?: return

        val builder = context.irBuiltIns.createIrBuilder(container.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        for (declaration in container.declarations) {
            when (declaration) {
                is IrEnumEntry -> {
                    declaration.getInstanceFun?.let { getInstance ->
                        builder.insertCall(getInstance, staticInitFunction)
                    }
                }
                // Do not insert call to a static_init into static_init itself
                is IrSimpleFunction if declaration == staticInitFunction -> continue
                // Do not insert a call to a static_init into an enum constructor, since it would be only accessible from static_init.
                // Redundant re-entrance into static_init pollutes stepping.
                is IrConstructor if (container.isEnumClass || container.isEnumEntry) -> continue
                is IrFunction -> {
                    // If initializeObjectEnumParent is true, call static_init from all objects getInstance
                    // including nested objects. This behavior is K/JS-only and differs from JVM. Kept for compatibility.
                    // Please see KT-83337.
                    if (!initializeContainerOfInnerObject && declaration.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION) continue
                    if (declaration.dispatchReceiverParameter != null) continue // already initialized when instance was created
                    builder.insertCall(declaration, staticInitFunction)
                }
                // If initializeObjectEnumParent is false, only call static_init from getInstance coming from the companion object.
                // JVM-based behavior, also relevant for Wasm.
                is IrClass if declaration.isCompanion && !initializeContainerOfInnerObject -> {
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
