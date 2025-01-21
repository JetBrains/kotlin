/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.ClassFakeOverrideReplacement
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.transformInPlace

/**
 * Generates default compatibility bridges for classes in `-jvm-default=enable/no-compatibility` modes.
 * See [org.jetbrains.kotlin.backend.jvm.ClassFakeOverrideReplacement.DefaultCompatibilityBridge].
 */
@PhaseDescription(name = "GenerateJvmDefaultCompatibilityBridges")
class GenerateJvmDefaultCompatibilityBridges(private val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!context.config.jvmDefaultMode.isEnabled) return

        if (irClass.isJvmInterface) return

        irClass.declarations.transformInPlace { declaration ->
            replaceWithDefaultCompatibilityBridgeIfNeeded(declaration) ?: declaration
        }
    }

    private fun replaceWithDefaultCompatibilityBridgeIfNeeded(declaration: IrDeclaration): IrDeclaration? {
        if (declaration !is IrSimpleFunction) return null
        val (newFunction, superFunction) = context.cachedDeclarations.getClassFakeOverrideReplacement(declaration)
                as? ClassFakeOverrideReplacement.DefaultCompatibilityBridge ?: return null
        newFunction.generateBridgeBody(declaration, superFunction)
        return newFunction
    }

    private fun IrSimpleFunction.generateBridgeBody(declaration: IrSimpleFunction, superFunction: IrSimpleFunction) {
        val offset = declaration.parentAsClass.startOffset
        context.createIrBuilder(symbol, offset, offset).apply {
            body = irExprBody(irBlock {
                +irCall(superFunction.symbol, returnType).apply {
                    superQualifierSymbol = superFunction.parentAsClass.symbol

                    dispatchReceiver = irGet(dispatchReceiverParameter!!)
                    extensionReceiverParameter?.let { extensionReceiver = irGet(it) }
                    for ((index, parameter) in typeParameters.withIndex()) {
                        typeArguments[index] = parameter.defaultType
                    }
                    for ((index, parameter) in valueParameters.withIndex()) {
                        putValueArgument(index, irGet(parameter))
                    }
                }
            })
        }
    }
}
