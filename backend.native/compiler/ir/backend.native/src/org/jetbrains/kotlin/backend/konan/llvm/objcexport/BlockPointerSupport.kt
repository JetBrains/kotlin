/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.objcexport.BlockPointerBridge
import org.jetbrains.kotlin.descriptors.konan.CurrentKonanModuleOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun ObjCExportCodeGenerator.generateBlockToKotlinFunctionConverter(
        bridge: BlockPointerBridge
): LLVMValueRef {
    val irInterface = symbols.functions[bridge.numberOfParameters].owner
    val invokeMethod = irInterface.declarations.filterIsInstance<IrSimpleFunction>()
            .single { it.name == OperatorNameConventions.INVOKE }

    // Note: we can store Objective-C block pointer as associated object of Kotlin function object itself,
    // but only if it is equivalent to its dynamic translation result. If block returns void, then it's not like that:
    val useSeparateHolder = bridge.returnsVoid

    val bodyType = if (useSeparateHolder) {
        structType(codegen.kObjHeader, codegen.kObjHeaderPtr)
    } else {
        structType(codegen.kObjHeader)
    }

    val invokeImpl = generateFunction(
            codegen,
            codegen.getLlvmFunctionType(invokeMethod),
            "invokeFunction${bridge.nameSuffix}"
    ) {
        val args = (0 until bridge.numberOfParameters).map { index ->
            kotlinReferenceToObjC(param(index + 1))
        }

        val thisRef = param(0)
        val associatedObjectHolder = if (useSeparateHolder) {
            val bodyPtr = bitcast(pointerType(bodyType), thisRef)
            loadSlot(structGep(bodyPtr, 1), isVar = false)
        } else {
            thisRef
        }
        val blockPtr = callFromBridge(
                context.llvm.Kotlin_ObjCExport_GetAssociatedObject,
                listOf(associatedObjectHolder)
        )

        val invoke = loadBlockInvoke(blockPtr, bridge)
        val result = callFromBridge(invoke, listOf(blockPtr) + args)

        val kotlinResult = if (bridge.returnsVoid) {
            theUnitInstanceRef.llvm
        } else {
            objCReferenceToKotlin(result, Lifetime.RETURN_VALUE)
        }
        ret(kotlinResult)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    val typeInfo = rttiGenerator.generateSyntheticInterfaceImpl(
            irInterface,
            mapOf(invokeMethod to constPointer(invokeImpl)),
            bodyType,
            immutable = true
    )

    return generateFunction(
            codegen,
            functionType(codegen.kObjHeaderPtr, false, int8TypePtr, codegen.kObjHeaderPtrPtr),
            ""
    ) {
        val blockPtr = param(0)
        ifThen(icmpEq(blockPtr, kNullInt8Ptr)) {
            ret(kNullObjHeaderPtr)
        }

        val retainedBlockPtr = callFromBridge(retainBlock, listOf(blockPtr))

        val result = if (useSeparateHolder) {
            val result = allocInstance(typeInfo.llvm, Lifetime.RETURN_VALUE)
            val bodyPtr = bitcast(pointerType(bodyType), result)
            val holder = allocInstanceWithAssociatedObject(
                    symbols.interopForeignObjCObject.owner.typeInfoPtr,
                    retainedBlockPtr,
                    Lifetime.ARGUMENT
            )
            storeHeapRef(holder, structGep(bodyPtr, 1))
            result
        } else {
            allocInstanceWithAssociatedObject(typeInfo, retainedBlockPtr, Lifetime.RETURN_VALUE)
        }

        ret(result)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }
}

private fun FunctionGenerationContext.loadBlockInvoke(
        blockPtr: LLVMValueRef,
        bridge: BlockPointerBridge
): LLVMValueRef {
    val blockLiteralType = codegen.runtime.getStructType("Block_literal_1")
    val invokePtr = structGep(bitcast(pointerType(blockLiteralType), blockPtr), 3)

    return bitcast(pointerType(bridge.blockInvokeLlvmType), load(invokePtr))
}

private fun FunctionGenerationContext.allocInstanceWithAssociatedObject(
        typeInfo: ConstPointer,
        associatedObject: LLVMValueRef,
        resultLifetime: Lifetime
): LLVMValueRef = call(
        context.llvm.Kotlin_ObjCExport_AllocInstanceWithAssociatedObject,
        listOf(typeInfo.llvm, associatedObject),
        resultLifetime
)

private val BlockPointerBridge.blockInvokeLlvmType: LLVMTypeRef
    get() = functionType(
            if (returnsVoid) voidType else int8TypePtr,
            false,
            (0..numberOfParameters).map { int8TypePtr }
    )

private val BlockPointerBridge.nameSuffix: String
    get() = numberOfParameters.toString() + if (returnsVoid) "V" else ""

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
        storeHeapRef(kNullObjHeaderPtr, slot) // TODO: can dispose_helper write to the block?

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
        storeHeapRef(loadSlot(srcSlot, isVar = false), dstSlot)

        ret(null)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    fun org.jetbrains.kotlin.backend.konan.Context.LongInt(value: Long) =
            if (is64Bit()) Int64(value) else Int32(value.toInt())

    private fun generateDescriptorForBlockAdapterToFunction(bridge: BlockPointerBridge): ConstValue {
        val numberOfParameters = bridge.numberOfParameters

        val signature = buildString {
            append(if (bridge.returnsVoid) 'v' else '@')
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

        store(
                bitcast(int8TypePtr, value),
                bitcast(pointerType(int8TypePtr), slot)
        )
    }

    private fun ObjCExportCodeGenerator.generateInvoke(bridge: BlockPointerBridge): ConstPointer {
        val numberOfParameters = bridge.numberOfParameters

        val result = generateFunction(codegen, bridge.blockInvokeLlvmType, "invokeBlock${bridge.nameSuffix}") {
            val blockPtr = bitcast(pointerType(blockLiteralType), param(0))
            val kotlinFunction = loadSlot(structGep(blockPtr, 1), isVar = false)

            val args = (1 .. numberOfParameters).map { index ->
                objCReferenceToKotlin(param(index), Lifetime.ARGUMENT)
            }

            val invokeMethod = context.ir.symbols.functions[numberOfParameters].owner.declarations
                    .filterIsInstance<IrSimpleFunction>().single { it.name == OperatorNameConventions.INVOKE }

            val callee = lookupVirtualImpl(kotlinFunction, invokeMethod)

            val result = callFromBridge(callee, listOf(kotlinFunction) + args, Lifetime.ARGUMENT)

            if (bridge.returnsVoid) {
                ret(null)
            } else {
                ret(kotlinReferenceToObjC(result))
            }
        }.also {
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
        }

        return constPointer(result)
    }

    fun ObjCExportCodeGenerator.generateConvertFunctionToBlock(bridge: BlockPointerBridge): LLVMValueRef {
        val blockDescriptor = codegen.staticData.placeGlobal(
                "",
                generateDescriptorForBlockAdapterToFunction(bridge)
        )

        return generateFunction(
                codegen,
                functionType(int8TypePtr, false, codegen.kObjHeaderPtr),
                "convertFunction${bridge.nameSuffix}"
        ) {
            val kotlinRef = param(0)
            ifThen(icmpEq(kotlinRef, kNullObjHeaderPtr)) {
                ret(kNullInt8Ptr)
            }

            val isa = codegen.importGlobal(
                    "_NSConcreteStackBlock",
                    int8TypePtr,
                    CurrentKonanModuleOrigin
            )

            val flags = Int32((1 shl 25) or (1 shl 30) or (1 shl 31)).llvm
            val reserved = Int32(0).llvm

            val invokeType = pointerType(functionType(voidType, true, int8TypePtr))
            val invoke = generateInvoke(bridge).bitcast(invokeType).llvm
            val descriptor = blockDescriptor.llvmGlobal

            val blockOnStack = alloca(blockLiteralType)
            val blockOnStackBase = structGep(blockOnStack, 0)
            val slot = structGep(blockOnStack, 1)

            listOf(bitcast(int8TypePtr, isa), flags, reserved, invoke, descriptor).forEachIndexed { index, value ->
                // Although value is actually on the stack, it's not in normal slot area, so we cannot handle it
                // as if it was on the stack.
                store(value, structGep(blockOnStackBase, index))
            }

            // Note: it is the slot in the block located on stack, so no need to manage it properly:
            storeRefUnsafe(kotlinRef, slot)

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

private val ObjCExportCodeGenerator.retainBlock get() = context.llvm.externalFunction(
        "objc_retainBlock",
        functionType(int8TypePtr, false, int8TypePtr),
        CurrentKonanModuleOrigin
)