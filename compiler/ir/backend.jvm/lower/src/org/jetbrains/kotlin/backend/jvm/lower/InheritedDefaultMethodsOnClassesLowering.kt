/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.ClassFakeOverrideReplacement
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createDefaultImplsRedirection
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createPlaceholderAnyNType
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.util.*

/**
 * Adds bridge implementations in classes that inherit default implementations from interfaces.
 */
@PhaseDescription(name = "InheritedDefaultMethodsOnClasses")
internal class InheritedDefaultMethodsOnClassesLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isJvmInterface) {
            irClass.declarations.transformInPlace {
                transformMemberDeclaration(it)
            }
        }
    }

    private fun transformMemberDeclaration(declaration: IrDeclaration): IrDeclaration {
        if (declaration !is IrSimpleFunction) return declaration

        if (declaration.isFakeOverride && declaration.name.asString() == "clone") {
            val overriddenFunctions = declaration.allOverridden(false)
            val cloneFun = overriddenFunctions.find { it.parentAsClass.hasEqualFqName(StandardNames.FqNames.cloneable.toSafe()) }
            if (cloneFun != null && overriddenFunctions.all { it.isFakeOverride || it == cloneFun }) {
                return generateCloneImplementation(declaration, cloneFun)
            }
        }

        val (newFunction, superFunction) =
            context.cachedDeclarations.getClassFakeOverrideReplacement(declaration) as? ClassFakeOverrideReplacement.DefaultImplsRedirection
                ?: return declaration
        return generateDefaultImplsRedirectionBody(newFunction, superFunction)
    }

    private fun generateCloneImplementation(fakeOverride: IrSimpleFunction, cloneFun: IrSimpleFunction): IrSimpleFunction {
        assert(fakeOverride.isFakeOverride)
        val irFunction = context.irFactory.createDefaultImplsRedirection(fakeOverride)
        val offset = fakeOverride.parentAsClass.startOffset
        context.createJvmIrBuilder(irFunction.symbol, offset, offset).apply {
            irFunction.body = irBlockBody {
                +irReturn(
                    irCall(cloneFun, origin = null, superQualifierSymbol = cloneFun.parentAsClass.symbol).apply {
                        dispatchReceiver = irGet(irFunction.dispatchReceiverParameter!!)
                    }
                )
            }
        }
        return irFunction
    }

    private fun generateDefaultImplsRedirectionBody(
        irFunction: IrSimpleFunction,
        superFunction: IrSimpleFunction,
    ): IrSimpleFunction {
        val defaultImplFun = context.cachedDeclarations.getDefaultImplsFunction(superFunction)
        val offset = irFunction.parentAsClass.startOffset
        val backendContext = context
        context.createIrBuilder(irFunction.symbol, offset, offset).apply {
            irFunction.body = irExprBody(irBlock {
                val parameter2arguments = backendContext.multiFieldValueClassReplacements
                    .mapFunctionMfvcStructures(this, defaultImplFun, irFunction) { sourceParameter, _ ->
                        irGet(sourceParameter).let {
                            if (sourceParameter != irFunction.dispatchReceiverParameter) it
                            else it.reinterpretAsDispatchReceiverOfType(superFunction.parentAsClass.defaultType)
                        }
                    }
                +irCall(defaultImplFun.symbol, irFunction.returnType).apply {
                    for (index in superFunction.parentAsClass.typeParameters.indices) {
                        typeArguments[index] = createPlaceholderAnyNType(context.irBuiltIns)
                    }
                    passTypeArgumentsFrom(irFunction, offset = superFunction.parentAsClass.typeParameters.size)

                    for ((parameter, argument) in parameter2arguments) {
                        if (argument != null) {
                            putArgument(parameter, argument)
                        }
                    }
                }
            })
        }

        return irFunction
    }
}
