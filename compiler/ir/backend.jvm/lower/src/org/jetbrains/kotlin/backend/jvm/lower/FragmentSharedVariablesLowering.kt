/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// Used from the IntelliJ IDEA Kotlin Debugger Plug-In
@Suppress("unused")
val fragmentSharedVariablesLowering = makeIrModulePhase(
    ::FragmentSharedVariablesLowering,
    name = "FragmentSharedVariablesLowering",
    description = "Promotes captured variables that are modified by the fragment to shared variables"
)

// This lowering is a preprocessor for IR in order to support the compilation
// scheme used by the "Evaluate Expression..." mechanism of the IntelliJ plug-in
// for Kotlin debugging.
//
// Fragments are compiled as the body of an enclosing function that close the free
// variables of the fragment as parameters. The values of these are then extracted
// from the stack at the current breakpoint, and the fragment code is invoked with
// these values to evaluate the expression.
//
// If the parameter is a shared variable, e.g. `IntRef` (the same mechanism used to
// implement captures of lambdas) the value extracted from the stack is
// automatically boxed in a `Ref` before being passed to the fragment.
//
// Upon return, all `Ref`s are written back into the stack, thus allowing fragments
// to modify the state of the program being debugged.
//
// This lowering promotes these parameters to `Ref`s, as deemed appropriate by
// psi2ir's Fragment generation.
//
// The reason for this "phasing" is that the JVM specific infrastructure (e.g.
// symbols for `Ref`s) have not been loaded when psi2ir runs, as psi2ir is designed
// to be backend agnostic. So, we "tag" the appropriate parameters with a new
// JvmIrDeclarationOrigin that we can then detect in this lowering.
//
// See `FragmentDeclarationGenerator.kt:declareParameter` for the front half
// of this logic.
class FragmentSharedVariablesLowering(
    val context: JvmBackendContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    companion object {
        // Echo of GENERATED_FUNCTION_NAME in the JVM Debugger plug-in.
        // TODO: Find a good common dependency of JVM Debugger and IR Compiler and deduplicate this
        const val GENERATED_FUNCTION_NAME = "generated_for_debugger_fun"
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (declaration.name.asString() != GENERATED_FUNCTION_NAME) {
            return super.visitFunctionNew(declaration)
        }

        val promotedParameters = promoteParametersForCapturesToRefs(declaration)
        replaceUseOfPromotedParametersWithRefs(declaration, promotedParameters)
        return declaration
    }

    private fun promoteParametersForCapturesToRefs(declaration: IrFunction): Map<IrValueParameterSymbol, IrValueParameterSymbol> {
        val promotedParameters = mutableMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
        declaration.valueParameters = declaration.valueParameters.map {
            if (it.origin == IrDeclarationOrigin.SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT) {
                val newParameter =
                    it.copyTo(
                        declaration,
                        type = context.sharedVariablesManager.getIrType(it.type),
                        origin = IrDeclarationOrigin.DEFINED
                    )
                promotedParameters[it.symbol] = newParameter.symbol
                newParameter
            } else {
                it
            }
        }
        return promotedParameters
    }

    private fun replaceUseOfPromotedParametersWithRefs(
        declaration: IrFunction,
        promotedParameters: Map<IrValueParameterSymbol, IrValueParameterSymbol>
    ) {
        declaration.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                expression.transformChildrenVoid(this)
                val newDeclaration = promotedParameters[expression.symbol] ?: return expression
                return context.sharedVariablesManager.getSharedValue(newDeclaration, expression)
            }

            override fun visitSetValue(expression: IrSetValue): IrExpression {
                expression.transformChildrenVoid(this)
                val newDeclaration = promotedParameters[expression.symbol] ?: return expression
                return context.sharedVariablesManager.setSharedValue(newDeclaration, expression)
            }
        })
    }
}
