/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

@PhasePrerequisites(
    ObjectDeclarationLowering::class,
    EnumEntryInstancesLowering::class,
    EnumEntryCreateGetInstancesFunsLowering::class,
)
class WasmStaticInitializersDeclarationLowering(override val context: WasmBackendContext) : WebStaticInitializersDeclarationLowering() {
    private fun IrBuilderWithScope.generateStaticInitializationFailureCallWithClassName(container: IrClass): IrCall =
        irCall(this@WasmStaticInitializersDeclarationLowering.context.symbols.staticInitializationFailureWithClassName).apply {
            arguments[0] = kClassReference(container.symbol.starProjectedType)
        }

    protected override fun createStaticInitFunction(
        container: IrClass,
        origin: IrDeclarationOrigin,
        initCalledVar: IrField,
        initializers: List<IrStatement>
    ): IrSimpleFunction {
        val initFunction = context.irFactory.buildFun {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            this.origin = origin
            name = Name.identifier(STATIC_INIT_FUNCTION_NAME)
            visibility = DescriptorVisibilities.PUBLIC
            returnType = context.irBuiltIns.unitType
        }
        return initFunction.apply {
            val builder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET)
            parent = container
            body = context.irFactory.createBlockBody(startOffset, endOffset) {
                with(builder) {
                    // Need a temporary variable for Wasm to transform if/then branches with br_table
                    val initState = scope.createTemporaryVariable(
                        irGetField(null, initCalledVar),
                        nameHint = "initState",
                        inventUniqueName = false,
                    )
                    statements += initState
                    statements += irWhen(
                        context.irBuiltIns.unitType,
                        listOf(
                            // Already initialized successfully - early branch.
                            irBranch(
                                irEqeqeq(irGet(initState), irInt(InitializationState.INITIALIZED)),
                                irReturnUnit()
                            ),
                            // Previously attempted initialization failed with error.
                            irBranch(
                                irEqeqeq(irGet(initState), irInt(InitializationState.ERROR)),
                                generateStaticInitializationFailureCallWithClassName(container)
                            ),
                            // Initialization hasn't been performed yet - try to initialize.
                            irElseBranch(
                                initializationBody(initFunction, container, initCalledVar, initializers)
                            )
                        )
                    )
                }
            }
        }
    }
}
