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
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.ir.validation.withBasicChecks
import org.jetbrains.kotlin.name.Name

class SplitConstructorsIntoAllocationInitialization(val backendContext: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
//        return
        if (container is IrConstructor) {
            val originalConstructor = container

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
            )

            // TODO this seems wrong, should be deep copies for both of these, right?
            initializeDontAllocateFunction.annotations = originalConstructor.annotations
            initializeDontAllocateFunction.typeParameters = originalConstructor.typeParameters

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

            for (param in originalConstructor.parameters) {
                initializeDontAllocateFunction.parameters += param.deepCopyWithSymbols(initializeDontAllocateFunction)
                newConstructorInitializeDoAllocate.parameters += param.deepCopyWithSymbols(newConstructorInitializeDoAllocate)
            }

            // constructors don't have dispatch receivers, but member functions do, so add it here
            assert(originalConstructor.dispatchReceiverParameter == null)
            assert(newConstructorInitializeDoAllocate.dispatchReceiverParameter == null)
            assert(initializeDontAllocateFunction.dispatchReceiverParameter != null)

            val irb = backendContext.createIrBuilder(
                newConstructorInitializeDoAllocate.symbol,
                originalConstructor.startOffset,
                originalConstructor.endOffset
            )

            newConstructorInitializeDoAllocate.body = irb.irBlockBody {
                +irCall(backendContext.wasmSymbols.wasmAllocateGCObject)
                val returnedThis = irCall(initializeDontAllocateFunction.symbol)
                // TODO fundamental showstoper problem: can't add the this argument to the call (even though its needed), because it doesn't exist yet
//                returnedThis.dispatchReceiver = irGet(initializeDontAllocateFunction.dispatchReceiverParameter!!)
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