package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

// Type.CHAR_TYPE -> "Char"
fun getKotlinPrimitiveClassName(type: Type): Name {
    return JvmPrimitiveType.get(type.className).primitiveType.typeName
}

// "Char" -> type for kotlin.collections.CharIterator
fun getPrimitiveIteratorType(primitiveClassName: Name): Type {
    val iteratorName = Name.identifier(primitiveClassName.asString() + "Iterator")
    return Type.getObjectType(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME.child(iteratorName).internalNameWithoutInnerClasses)
}

fun getWrappedArray(instructionAdapter: InstructionAdapter, elementType: Type) {
    instructionAdapter.invokevirtual(vArrayWrapperType, "getArray", "()Ljava/lang/Object;", false)
    instructionAdapter.checkcast(AsmUtil.getArrayType(elementType))
}

const val vArrayWrapperType = "kotlin/VArrayWrapper"