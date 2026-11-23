/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.IrInlinableLambda
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.getSourceLocation
import org.jetbrains.kotlin.ir.backend.js.utils.findUnitGetInstanceFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.isAncestorOf

private fun addShadowStackTracking(ctx: WasmBackendContext, irFunction: IrFunction){
    // ========= TODO ========== TEst / Make this whole thing work with incremental compilation
    if(irFunction == ctx.findUnitGetInstanceFunction())
        // makes type problems TODO understand better
        return

//    if(irFunction.symbol in listOf(ctx.wasmSymbols.wasmShadowStackPush, ctx.wasmSymbols.wasmShadowStackPop))
    if(irFunction.getPackageFragment().packageFqName.asString() == "kotlin.wasm.internal")
        // don't endlessly recurse
        return

    val body = irFunction.body!!
    // TODO always need to add to the global
    // TODO think about cases where we don't actually need the try-finally

    // Idea: Wrap every function* with try-finally
    // - try does:
    //      - global = global + id(f)
    //      - local = global
    // - finally does: pop from the global (alternative, more robust in case we omit the try-finally sometimes: reset global to local, then pop)
//    val transformer = object : IrElementTransformerVoid() {
//        override fun visitBody(body: IrBody): IrBody {


            val irb = ctx.createIrBuilder(irFunction.symbol, irFunction.startOffset, irFunction.endOffset)

            val tryBodyWrappingExisting = irb.irComposite {
                // NOTE: for now, we just save the function. This isn't as detailed a stacktrace as is possible (main thing that is missing: offsets to the exact callsite of each function, as is the standard in most stacktraces)
                // basically just +irCall, but inlined
                +ctx.symbols.wasmShadowStackPush.owner.inline(irFunction, moveBody = false)

                // TODO optionally, we could save the shadow stack locally here, and restore it using that, but we need to wrap every function anyway, so most likely there's no advantage to this
//                localStackBackup = irTemporary(irCall(ctx.symbols.wasmShadowStackGet), "LocalShadowStack", unit)

                +body.statements
                if(body is IrBlockBody) // TODO do this nicer, don't have another if for that here
//                    +irUnit()
                   +irGetObject(context.irBuiltIns.unitClass)
            }

    val restoreShadowStack = ctx.symbols.wasmShadowStackPop.owner.inline(irFunction, moveBody = false)
    val irTry = irb.irTry(type = tryBodyWrappingExisting.type, tryBodyWrappingExisting, catches = emptyList(), restoreShadowStack)

    when (body) {
        is IrBlockBody -> {
            body.statements.clear()
            body.statements.add(irTry)
        }
        is IrExpressionBody -> {
            body.expression = irTry
        }
        is IrSyntheticBody -> TODO("suitable error")
    }




//        }
//    }

//    irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
//        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
//            return super.visitConstructorCall(expression)
//        }
//    })

    // NOTE: we could also have done it with an extra parameter (which is automatically on the stack, so gets unwound automatically, basically), but editing parameters is quite hairy
    // TODO special case @WasmImports: surround the import call with try-finally, to push/pop a special indicator of an imported function,
}

class WasmWasiOptionalShadowStackLowering(val ctx: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO check whether we enable this feature, skip the lowering otherwise
//        if (!enabled) return
        if (ctx.isWasmJsTarget) return
        val irFunction = container as? IrFunction ?: return
        assert(irFunction.body == irBody)

        addShadowStackTracking(ctx, irFunction)
    }
}
