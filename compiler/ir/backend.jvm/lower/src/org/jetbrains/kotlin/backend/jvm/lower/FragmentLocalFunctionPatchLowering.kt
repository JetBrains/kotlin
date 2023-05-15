/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


// Used from CodeFragmentCompiler for IDE Debugger Plug-In
@Suppress("unused")
val fragmentLocalFunctionPatchLowering = makeIrFilePhase(
    ::FragmentLocalFunctionPatchLowering,
    name = "FragmentLocalFunctionPatching",
    description = "Rewrite calls to local functions to the appropriate, lifted function created by local declarations lowering.",
    prerequisite = setOf(localDeclarationsPhase)
)

// This lowering rewrites local function calls in code fragments to the
// corresponding lifted declaration. In the process, the lowering determines
// whether the captures of the local function are a subset of the captures of
// the fragment, and if not, introduces additional captures to the fragment
// wrapper. The captures are then supplied to the fragment wrapper as
// parameters supplied at evaluation time.
internal class FragmentLocalFunctionPatchLowering(
    val context: JvmBackendContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    lateinit var localDeclarationsData: Map<IrFunction, JvmBackendContext.LocalFunctionData>

    override fun lower(irFile: IrFile) {
        context.localDeclarationsLoweringData?.let {
            localDeclarationsData = it
        } ?: return
        irFile.transformChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        declaration.body?.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                val localDeclarationsDataKey = when (expression.symbol.owner.origin) {
                    is IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> context.mapping.defaultArgumentsOriginalFunction[expression.symbol.owner]
                    else -> expression.symbol.owner
                }
                val localsData = localDeclarationsData[localDeclarationsDataKey] ?: return super.visitCall(expression)
                val remappedTarget: LocalDeclarationsLowering.LocalFunctionContext = localsData.localContext

                val irBuilder = context.createJvmIrBuilder(declaration.symbol)
                return irBuilder.irCall(remappedTarget.transformedDeclaration).apply {
                    this.copyTypeArgumentsFrom(expression)
                    extensionReceiver = expression.extensionReceiver
                    dispatchReceiver = expression.dispatchReceiver

                    remappedTarget.transformedDeclaration.valueParameters.map { newValueParameterDeclaration ->
                        val oldParameter = localsData.newParameterToOld[newValueParameterDeclaration]

                        val getValue = if (oldParameter != null) {
                            // the parameter is an actual parameter to the local
                            // function, not a parameter corresponding to a
                            // capture introduced by a lowering: fetch
                            // the corresponding argument from the existing
                            // call and place at the appropriate slot in the
                            // call to the lowered function
                            expression.getValueArgument(oldParameter.index)!!
                        } else {
                            // The parameter is introduced by the lowering to
                            // private static function, so corresponds to a _capture_ by the local function
                            val capturedValueSymbol =
                                localsData.newParameterToCaptured[newValueParameterDeclaration]
                                    ?: error("Non-mapped parameter $newValueParameterDeclaration")

                            // We introduce a new parameter to the _fragment function_ surrounding the call to the
                            // lowered local function, and supply _that_ parameter to the corresponding _argument_ slot
                            // in the call to the lowered function.
                            val newParameter = declaration.addValueParameter {
                                type = capturedValueSymbol.owner.type
                                name = capturedValueSymbol.owner.name
                            }

                            context.state.recordNewFragmentCaptureParameter(
                                newParameter.name.asString(),
                                capturedValueSymbol.owner.type.toIrBasedKotlinType(),
                                capturedValueSymbol.owner.toIrBasedDescriptor()
                            )

                            irBuilder.irGet(newParameter)
                        }

                        putValueArgument(newValueParameterDeclaration.index, getValue)
                    }
                }
            }

        })

        return declaration
    }
}