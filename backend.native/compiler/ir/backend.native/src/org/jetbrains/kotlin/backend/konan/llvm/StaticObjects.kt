package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.replace

private fun StaticData.objHeader(typeInfo: ConstPointer): Struct {
    val containerOffsetNegative = 0 // Static object mark.
    return Struct(runtime.objHeaderType, typeInfo, Int32(containerOffsetNegative))
}

private fun StaticData.arrayHeader(typeInfo: ConstPointer, length: Int): Struct {
    assert (length >= 0)
    val containerOffsetNegative = 0 // Static object mark.
    return Struct(runtime.arrayHeaderType, typeInfo, Int32(containerOffsetNegative), Int32(length))
}

internal fun StaticData.createKotlinStringLiteral(value: IrConst<String>) = createKotlinStringLiteral(value.value)

internal fun StaticData.createKotlinStringLiteral(value: String): ConstPointer {
    val name = "kstr:" + value.globalHashBase64
    val elements = value.toByteArray(Charsets.UTF_8).map(::Int8)

    val objRef = createKotlinArray(KonanPlatform.builtIns.stringType, elements)

    val res = createAlias(name, objRef)
    LLVMSetLinkage(res.llvm, LLVMLinkage.LLVMWeakAnyLinkage)

    return res
}

private fun StaticData.createRef(type: KotlinType, objHeaderPtr: ConstPointer): ConstPointer {
    val llvmType = getLLVMType(type)
    return if (llvmType != objHeaderPtr.llvmType) {
        objHeaderPtr.bitcast(llvmType)
    } else {
        objHeaderPtr
    }
}

internal fun StaticData.createKotlinArray(arrayType: KotlinType, elements: List<LLVMValueRef>) =
        createKotlinArray(arrayType, elements.map { constValue(it) }).llvm

internal fun StaticData.createKotlinArray(arrayType: KotlinType, elements: List<ConstValue>): ConstPointer {

    val typeInfo = arrayType.typeInfoPtr!!

    val bodyElementType: LLVMTypeRef = elements.firstOrNull()?.llvmType ?: int8Type
    // (use [0 x i8] as body if there are no elements)
    val arrayBody = ConstArray(bodyElementType, elements)

    val compositeType = structType(runtime.arrayHeaderType, arrayBody.llvmType)

    val global = this.createGlobal(compositeType, "")

    val objHeaderPtr = global.pointer.getElementPtr(0)
    val arrayHeader = arrayHeader(typeInfo, elements.size)

    global.setInitializer(Struct(compositeType, arrayHeader, arrayBody))

    return createRef(arrayType, objHeaderPtr)
}

internal fun StaticData.createKotlinObject(type: KotlinType, body: ConstValue): ConstPointer {
    val typeInfo = type.typeInfoPtr!!

    val compositeType = structType(runtime.objHeaderType, body.llvmType)

    val global = this.createGlobal(compositeType, "")

    val objHeaderPtr = global.pointer.getElementPtr(0)
    val objHeader = objHeader(typeInfo)

    global.setInitializer(Struct(compositeType, objHeader, body))

    return createRef(type, objHeaderPtr)
}

private fun StaticData.getArrayListClass(): ClassDescriptor {
    val module = context.irModule!!.descriptor
    val pkg = module.getPackage(FqName.fromSegments(listOf("kotlin", "collections")))
    val classifier = pkg.memberScope.getContributedClassifier(Name.identifier("ArrayList"),
            NoLookupLocation.FROM_BACKEND)

    return classifier as ClassDescriptor
}

/**
 * Creates static instance of `kotlin.collections.ArrayList<elementType>` with given values of fields.
 *
 * @param array value for `array: Array<E>` field.
 * @param length value for `length: Int` field.
 */
internal fun StaticData.createArrayList(elementType: TypeProjection, array: ConstPointer, length: Int): ConstPointer {
    val arrayListClass = getArrayListClass()

    // type is ArrayList<elementType>:
    val type = arrayListClass.defaultType.replace(listOf(elementType))

    // FIXME: properly import body type from stdlib

    val body = Struct(
            array, // array: Array<E>
            Int32(0), // offset: Int
            Int32(length), // length: Int
            NullPointer(kObjHeader) // backing: ArrayList<E>?
    )

    return createKotlinObject(type, body)
}
