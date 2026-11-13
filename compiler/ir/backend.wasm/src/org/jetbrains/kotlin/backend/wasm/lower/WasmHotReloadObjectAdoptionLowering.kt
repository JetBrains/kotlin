/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.HotReloadPayloads
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * defvar semantics for object state holders: the store that initializes an
 * object's (or companion's) instance field becomes
 * `prev = __hot_adopt$<tag>(); if (prev == null) <original store> else field = prev`
 * — CL `defvar` / JVM `<clinit>` semantics, rebuilt on the wasm constraint
 * that a fresh module's value cell starts empty: the HOST supplies the
 * `boundp` check as a host import, handing back the PREVIOUS live
 * instance's getter (the name carries the getter's `#<hashCode>` suffix
 * — the one naming piece stable across klib round trips AND edits of
 * the declaring file — so the host finds the previous live
 * `__hot_entry$…#<suffix>` export) or null when no previous binding
 * exists (cold boot, a brand-new object).
 * The initializer is the cold-boot default only, never re-run while a
 * binding exists. The engine checks the returned instance against the
 * object's cell type at the JS boundary, so an incompatible holder
 * change is engine-gated: the sweep rejects the new shape's swaps like
 * any cell-chain change (old code keeps running), and any live path
 * that drives the new code's holder access traps at that boundary. A
 * field added to an adopted instance migrates via the payload stepper
 * on first access, like any held instance.
 *
 * Both initialization regimes funnel through this one store: pure
 * objects in the hoisted file initializer, impure ones in the
 * constructor (lazy, at first getInstance) — and the wasm backend can
 * make either lazy, so the guard must be correct at arbitrary call
 * time (the host captures the previous VALUE eagerly, before the sweep
 * rewires the previous getter's slot).
 */
class WasmHotReloadObjectAdoptionLowering(private val context: WasmBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        if (!context.isHotReloadCompilation) return

        val adoptFunctions = mutableMapOf<IrClass, IrSimpleFunction>()
        val newFunctions = mutableListOf<IrSimpleFunction>()

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSetField(expression: IrSetField): IrExpression {
                expression.transformChildrenVoid(this)
                val field = expression.symbol.owner
                if (!field.isObjectInstanceField()) return expression
                val obj = field.parent as? IrClass ?: return expression
                if (obj.isExternal) return expression
                val getInstance = obj.objectGetInstanceFunction ?: return expression
                val adopt = adoptFunctions.getOrPut(obj) {
                    val referenceKey = context.idSignatureRetriever.declarationSignature(getInstance)!!
                    // restrictTo mirrors FieldInitializersLowering: a function
                    // created mid-lowering needs a signature context
                    context.irFactory.stageController.restrictTo(getInstance) {
                        context.irFactory.buildFun {
                            name = Name.identifier(HotReloadPayloads.hotAdoptFunctionName(obj.name.asString(), referenceKey))
                            origin = HOT_ADOPT_FUNCTION
                            returnType = obj.defaultType.makeNullable()
                            isExternal = true
                            visibility = DescriptorVisibilities.PUBLIC
                            modality = Modality.FINAL
                        }
                    }.also { function ->
                        function.parent = irFile
                        newFunctions.add(function)
                    }
                }
                with(context.createIrBuilder(expression.symbol)) {
                    return irBlock(resultType = context.irBuiltIns.unitType) {
                        val previous = irTemporary(irCall(adopt.symbol, adopt.returnType), nameHint = "hotAdopted")
                        +irIfNull(
                            context.irBuiltIns.unitType,
                            irGet(previous),
                            expression,                                    // no previous binding: cold init
                            irSetField(null, field, irGet(previous)),      // adopt the live instance
                        )
                    }
                }
            }
        })

        irFile.declarations.addAll(newFunctions)
    }
}

private val HOT_ADOPT_FUNCTION by IrDeclarationOriginImpl.Regular
