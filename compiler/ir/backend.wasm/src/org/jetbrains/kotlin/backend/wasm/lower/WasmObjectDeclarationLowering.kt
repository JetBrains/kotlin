/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.ObjectDeclarationLowering
import org.jetbrains.kotlin.ir.backend.js.lower.WebStaticInitializersDeclarationLowering
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.name.Name

/**
 * For an object `O`, generates the following:
 *
 * ```kotlin
 * class O {
 *
 *   /*static*/ var instance: O?
 *   /*static*/ fun getInstance(): O {
 *       if (!static_init_state) return this.instance /* implicitly cast to O */
 *       if (static_init_state == 2) {
 *         staticInitializationFailureWithClassName(O::class)
 *       }
 *       static_init_state = 0
 *       try {
 *         O()
 *       } catch (reason: Throwable) {
 *         static_init_state = 2
 *         kotlin.internal.staticInitializationFailure(reason, null)
 *       }
 *     return this.instance /* implicitly cast to O */
 *   }
 * }
 * ```
 *
 * Note that the error state is stored in the instance field itself.
 * This is done as a code size optimization and should not affect performance.
 */
@PhasePrerequisites(WasmCallableReferenceLowering::class)
class WasmObjectDeclarationLowering(context: WasmBackendContext) : ObjectDeclarationLowering<WasmBackendContext>(context) {
    companion object {
        private val STATE_FIELD_NAME = Name.identifier("object_init_state")
    }

    override val initializationGenerator = WasmLazyGlobalInitializationGenerator(context)

    override fun IrBlockBodyBuilder.generateLazyInitialization(
        instanceField: IrField,
        declaration: IrClass,
        primaryConstructorCall: IrExpression,
    ) {
        with(initializationGenerator) {
            val stateField = createStateField(STATE_FIELD_NAME, WebStaticInitializersDeclarationLowering.STATIC_CLASS_INITIALIZER)
            stateField.parent = declaration
            declaration.declarations.add(0, stateField)
            generateStaticInitializerBody(
                stateField,
                declaration,
                listOf(primaryConstructorCall),
                returnValue = irGetField(null, instanceField),
            )
        }
    }
}
