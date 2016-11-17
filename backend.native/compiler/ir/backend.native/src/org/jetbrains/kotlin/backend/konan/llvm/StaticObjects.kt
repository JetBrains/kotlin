package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

private fun StaticData.objHeader(containerOffsetNegative: Int, typeInfo: ConstPointer): Struct {
    assert (containerOffsetNegative >= 0)
    return Struct(runtime.objHeaderType, typeInfo, Int32(containerOffsetNegative))
}

private fun StaticData.arrayHeader(containerOffsetNegative: Int, typeInfo: ConstPointer, length: Int): Struct {
    assert (length >= 0)
    val objHeader = objHeader(containerOffsetNegative, typeInfo)
    return Struct(runtime.arrayHeaderType, objHeader, Int32(length))
}

private fun StaticData.staticContainerHeader(): Struct {
    val CONTAINER_TAG_NOCOUNT = 1 // FIXME: copy-pasted from runtime
    return Struct(runtime.containerHeaderType, Int32(CONTAINER_TAG_NOCOUNT))
}

internal fun StaticData.createKotlinStringLiteral(value: IrConst<String>): ConstPointer {
    val base64Str = value.value.globalHashBase64
    val valueBytes = value.value.toByteArray(Charsets.UTF_8)

    val arrayCount = valueBytes.size
    val arrayType = LLVMArrayType(int8Type, arrayCount)

    val compositeType = structType(runtime.containerHeaderType, runtime.arrayHeaderType, arrayType)

    // TODO: use C types alignments instead of LLVM ones
    val global = this.createGlobal(compositeType, "kstrcont:$base64Str")
    LLVMSetLinkage(global.llvmGlobal, LLVMLinkage.LLVMPrivateLinkage)

    val containerHeader = staticContainerHeader()

    val objHeaderPtr = global.pointer.getElementPtr(1)
    val containerHeaderPtr = global.pointer.getElementPtr(0)
    val containerOffsetNegative = objHeaderPtr.sub(containerHeaderPtr)

    val stringClass = value.type.constructor.declarationDescriptor as ClassDescriptor
    assert (stringClass.fqNameSafe.asString() == "kotlin.String")
    val arrayHeader = arrayHeader(containerOffsetNegative, stringClass.llvmTypeInfoPtr, arrayCount)

    val array = ConstArray(int8Type, valueBytes.map { Int8(it) } )

    global.setInitializer(Struct(compositeType, containerHeader, arrayHeader, array))

    val stringRef = objHeaderPtr.bitcast(getLLVMType(value.type))
    val res = createAlias("kstr:$base64Str", stringRef)
    LLVMSetLinkage(res.getLlvmValue(), LLVMLinkage.LLVMWeakAnyLinkage)

    return res
}
