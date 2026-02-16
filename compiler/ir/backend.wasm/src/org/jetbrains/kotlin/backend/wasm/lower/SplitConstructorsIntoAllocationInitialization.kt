/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.utils.primaryConstructorReplacement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.copyAnnotationsFrom
import org.jetbrains.kotlin.ir.util.copyParametersFrom
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameter
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.ir.validation.withBasicChecks
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import kotlin.math.exp

// TODO first run split constructors , then run rewrite

class SplitConstructorsIntoAllocationInitialization(val backendContext: WasmBackendContext) : BodyLoweringPass {
    // TODO make available outside, serialize for incremental compilation, etc.
    //       also need to share it with the second lowering somehow
//           some kind of map from constructors to init functions. need to think about what exactly we map, whether its the old constructor before the rewriting, or the new one, etc.
    // TODO these maps are in WasmBackendContext for now, and get filled here, see how this can best be linked/explained

    override fun lower(irBody: IrBody, container: IrDeclaration) {
//        return
        if (container is IrConstructor) {
            val originalConstructor = container

            val fileContext = backendContext.getFileContext(container.file)

            // very rough first idea: transform it into fn init + constructor (name new or smth, figure names out later), init just initializes, constructor also allocates
            // the observable behavior of the constructor shouldn't change in the ned
            val irClass = originalConstructor.parent as IrClass

            // TODO could use irClass.transformDeclarationsFlat, but thats more complicated, and less performant, because it iterates all decls

            // TODO names
            val initializeDontAllocateFunction = irClass.addFunction(
                startOffset = originalConstructor.startOffset,
                endOffset = originalConstructor.endOffset,
                origin = originalConstructor.origin,
                name = "${originalConstructor.name.asString()}_initNoAlloc",
                visibility = originalConstructor.visibility,
                isInline = originalConstructor.isInline,
                // TODO isExpect?
//                isExpect = originalConstructor.isExpect,
                returnType = originalConstructor.returnType,
//                isTailrec = false,
                isSuspend = false,
//                isOperator = false,
//                isInfix = false,
//                isExternal = false,
                isFakeOverride = false,
                // use a static function that has an explicit this parameter for now
                isStatic = true,
            )

            fileContext.originalCtorToInitNoAllocFnMap += originalConstructor.symbol to initializeDontAllocateFunction.symbol

            initializeDontAllocateFunction.body = originalConstructor.body!!.deepCopyWithSymbols(initializeDontAllocateFunction)

            // TODO missing anything else that needs to be copied?

            val newConstructorInitializeDoAllocate = irClass.addConstructor {
                startOffset = originalConstructor.startOffset
                endOffset = originalConstructor.endOffset
                origin = originalConstructor.origin
                name = originalConstructor.name
                visibility = originalConstructor.visibility
                isInline = originalConstructor.isInline
                isExpect = originalConstructor.isExpect
                returnType = originalConstructor.returnType
                isPrimary = originalConstructor.isPrimary
                isExternal = originalConstructor.isExternal
                containerSource = originalConstructor.containerSource
            }

            fileContext.splitCtorToOriginalCtorMap += originalConstructor.symbol to newConstructorInitializeDoAllocate.symbol

            assert(initializeDontAllocateFunction.parameters.isEmpty())


            // add the explicit `this` parameter for initNoAlloc, by creating it as a dispatch parameter and then rewriting it
            val dispatchReceiverParameter = initializeDontAllocateFunction.createDispatchReceiverParameterWithClassParent()
            val correctedParameter = dispatchReceiverParameter.also { it.kind = IrParameterKind.Regular }
            initializeDontAllocateFunction.parameters += correctedParameter

//            initializeDontAllocateFunction.parameters += backendContext.irFactory.createValueParameter(
//                startOffset = TODO(),
//                endOffset = TODO(),
//                origin = TODO(),
//                kind = TODO(),
//                name = TODO(),
//                type = TODO(),
//                isAssignable = TODO(),
//                symbol = TODO(),
//                varargElementType = TODO(),
//                isCrossinline = TODO(),
//                isNoinline = TODO(),
//                isHidden = TODO()
//            )


            // copy "signatures" to both new functions
            for (newlyCreated in listOf(newConstructorInitializeDoAllocate, initializeDontAllocateFunction)) {
                newlyCreated.copyAnnotationsFrom(originalConstructor)
                newlyCreated.copyTypeParametersFrom(originalConstructor)
                newlyCreated.copyParametersFrom(originalConstructor)
            }

            assert(originalConstructor.dispatchReceiverParameter == null)
            assert(newConstructorInitializeDoAllocate.dispatchReceiverParameter == null)
            assert(initializeDontAllocateFunction.dispatchReceiverParameter == null)

            val irb = backendContext.createIrBuilder(
                newConstructorInitializeDoAllocate.symbol,
                originalConstructor.startOffset,
                originalConstructor.endOffset
            )

            newConstructorInitializeDoAllocate.body = irb.irBlockBody {
                +irCall(backendContext.wasmSymbols.wasmAllocateGCObject)
                val returnedThis = irCall(initializeDontAllocateFunction.symbol)
                // TODO fundamental showstopper problem: can't add the this argument to the call (even though its needed), because it doesn't exist yet
                +irReturn(returnedThis)
            }

            // remove the old constructor
            // TODO would be nice to be able to use transform somehow for this instead
            // TODO this is not efficient, because addConstructor appends to the end, and we then remove from the beginning (well, most of the time it is in the beginning)
            irClass.declarations.remove(originalConstructor)

            assert(irClass.primaryConstructor != null)

            validateIr(
                container.parent,
                backendContext.irBuiltIns,
                IrValidatorConfig(true, true).withBasicChecks(),
                backendContext.messageCollector,
                IrVerificationMode.ERROR
            )
        }
    }

}

class RewriteConstructorCallsAfterSplit(val backendContext: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val fileContext = backendContext.getFileContext(container.file)



        // assert that (has delegating constructor call) implies (is constructor)
        // TODO maybe remove later because can't do it only in debug mode :(

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            // delegating constructors should delegate to the initNoAlloc function instead
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                val fnSymbol = fileContext.originalCtorToInitNoAllocFnMap[expression.symbol]
                // TODO write similar to irCall/irConstructorCall from irutils, but transforming ctor call into fn call
            }
            // normal constructor calls should point to the new one
            // TODO is there no better way? does this really have to be done manually?
        })
    }
}