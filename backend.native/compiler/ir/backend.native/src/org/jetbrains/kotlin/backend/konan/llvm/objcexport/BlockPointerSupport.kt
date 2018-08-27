/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.LLVMLinkage
import llvm.LLVMSetLinkage
import llvm.LLVMStoreSizeOfType
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.konan.CurrentKonanModuleOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun ObjCExportCodeGenerator.generateKotlinFunctionImpl(invokeMethod: IrSimpleFunction): ConstPointer {
    // TODO: consider also overriding methods of `Any`.

    val numberOfParameters = invokeMethod.valueParameters.size

    val function = generateFunction(
            codegen,
            codegen.getLlvmFunctionType(invokeMethod),
            "invokeFunction$numberOfParameters"
    ) {
        val args = (0 until numberOfParameters).map { index -> kotlinReferenceToObjC(param(index + 1)) }

        val rawBlockPtr = callFromBridge(context.llvm.Kotlin_ObjCExport_GetAssociatedObject, listOf(param(0)))

        val blockLiteralType = codegen.runtime.getStructType("Block_literal_1")
        val blockPtr = bitcast(pointerType(blockLiteralType), rawBlockPtr)
        val invokePtr = structGep(blockPtr, 3)

        val blockInvokeType = functionType(int8TypePtr, false, (0 .. numberOfParameters).map { int8TypePtr })

        val invoke = bitcast(pointerType(blockInvokeType), load(invokePtr))
        val result = callFromBridge(invoke, listOf(rawBlockPtr) + args)

        // TODO: support void-as-Unit.
        ret(objCReferenceToKotlin(result, Lifetime.RETURN_VALUE))
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    return constPointer(function)
}

internal class BlockAdapterToFunctionGenerator(val objCExportCodeGenerator: ObjCExportCodeGenerator) {
    private val codegen get() = objCExportCodeGenerator.codegen

    private val blockLiteralType = structType(
            codegen.runtime.getStructType("Block_literal_1"),
            codegen.kObjHeaderPtr
    )

    private val blockDescriptorType = codegen.runtime.getStructType("Block_descriptor_1")

    val disposeHelper = generateFunction(
            codegen,
            functionType(voidType, false, int8TypePtr),
            "blockDisposeHelper"
    ) {
        val blockPtr = bitcast(pointerType(blockLiteralType), param(0))
        val slot = structGep(blockPtr, 1)
        storeAny(kNullObjHeaderPtr, slot) // TODO: can dispose_helper write to the block?

        ret(null)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    val copyHelper = generateFunction(
            codegen,
            functionType(voidType, false, int8TypePtr, int8TypePtr),
            "blockCopyHelper"
    ) {
        val dstBlockPtr = bitcast(pointerType(blockLiteralType), param(0))
        val dstSlot = structGep(dstBlockPtr, 1)

        val srcBlockPtr = bitcast(pointerType(blockLiteralType), param(1))
        val srcSlot = structGep(srcBlockPtr, 1)

        // Kotlin reference was `memcpy`ed from src to dst, "revert" this:
        storeRefUnsafe(kNullObjHeaderPtr, dstSlot)
        // and copy properly:
        storeAny(loadSlot(srcSlot, isVar = false), dstSlot)

        ret(null)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    fun org.jetbrains.kotlin.backend.konan.Context.LongInt(value: Long) =
            if (is64Bit()) Int64(value) else Int32(value.toInt())

    private fun generateDescriptorForBlockAdapterToFunction(numberOfParameters: Int): ConstValue {
        val signature = buildString {
            append('@')
            val pointerSize = codegen.runtime.pointerSize
            append(pointerSize * (numberOfParameters + 1))

            var paramOffset = 0L

            (0 .. numberOfParameters).forEach { index ->
                append('@')
                if (index == 0) append('?')
                append(paramOffset)
                paramOffset += pointerSize
            }
        }

        return Struct(blockDescriptorType,
                codegen.context.LongInt(0L),
                codegen.context.LongInt(LLVMStoreSizeOfType(codegen.runtime.targetData, blockLiteralType)),
                constPointer(copyHelper),
                constPointer(disposeHelper),
                codegen.staticData.cStringLiteral(signature),
                NullPointer(int8Type)
        )
    }



    private fun FunctionGenerationContext.storeRefUnsafe(value: LLVMValueRef, slot: LLVMValueRef) {
        assert(value.type == kObjHeaderPtr)
        assert(slot.type == kObjHeaderPtrPtr)

        storeAny(
                bitcast(int8TypePtr, value),
                bitcast(pointerType(int8TypePtr), slot)
        )
    }

    private fun ObjCExportCodeGenerator.generateInvoke(numberOfParameters: Int): ConstPointer {
        val functionType = functionType(
                int8TypePtr,
                false,
                (0 .. numberOfParameters).map { int8TypePtr }
        )

        val result = generateFunction(codegen, functionType, "invokeBlock$numberOfParameters") {
            val blockPtr = bitcast(pointerType(blockLiteralType), param(0))
            val kotlinFunction = loadSlot(structGep(blockPtr, 1), isVar = false)

            val args = (1 .. numberOfParameters).map { index ->
                objCReferenceToKotlin(param(index), Lifetime.ARGUMENT)
            }

            val invokeMethod = context.ir.symbols.functions[numberOfParameters].owner.declarations
                    .filterIsInstance<IrSimpleFunction>().single { it.name == OperatorNameConventions.INVOKE }

            val callee = lookupVirtualImpl(kotlinFunction, invokeMethod)

            val result = callFromBridge(callee, listOf(kotlinFunction) + args, Lifetime.ARGUMENT)

            ret(kotlinReferenceToObjC(result))
        }.also {
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
        }

        return constPointer(result)
    }

    fun ObjCExportCodeGenerator.generateConvertFunctionToBlock(numberOfParameters: Int): LLVMValueRef {
        val blockDescriptor = codegen.staticData.placeGlobal(
                "",
                generateDescriptorForBlockAdapterToFunction(numberOfParameters)
        )

        return generateFunction(
                codegen,
                functionType(int8TypePtr, false, codegen.kObjHeaderPtr),
                "convertFunction$numberOfParameters"
        ) {
            val isa = codegen.importGlobal(
                    "_NSConcreteStackBlock",
                    int8TypePtr,
                    CurrentKonanModuleOrigin
            )

            val flags = Int32((1 shl 25) or (1 shl 30) or (1 shl 31)).llvm
            val reserved = Int32(0).llvm

            val invokeType = pointerType(functionType(voidType, true, int8TypePtr))
            val invoke = generateInvoke(numberOfParameters).bitcast(invokeType).llvm
            val descriptor = blockDescriptor.llvmGlobal

            val blockOnStack = alloca(blockLiteralType)
            val blockOnStackBase = structGep(blockOnStack, 0)
            val slot = structGep(blockOnStack, 1)

            listOf(bitcast(int8TypePtr, isa), flags, reserved, invoke, descriptor).forEachIndexed { index, value ->
                storeAny(value, structGep(blockOnStackBase, index))
            }

            // Note: it is the slot in the block located on stack, so no need to manage it properly:
            storeRefUnsafe(param(0), slot)

            val retainBlock = context.llvm.externalFunction(
                    "objc_retainBlock",
                    functionType(int8TypePtr, false, int8TypePtr),
                    CurrentKonanModuleOrigin
            )

            val copiedBlock = callFromBridge(retainBlock, listOf(bitcast(int8TypePtr, blockOnStack)))

            val autoreleaseReturnValue = context.llvm.externalFunction(
                    "objc_autoreleaseReturnValue",
                    functionType(int8TypePtr, false, int8TypePtr),
                    CurrentKonanModuleOrigin
            )

            ret(callFromBridge(autoreleaseReturnValue, listOf(copiedBlock)))
        }.also {
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
        }
    }
}
