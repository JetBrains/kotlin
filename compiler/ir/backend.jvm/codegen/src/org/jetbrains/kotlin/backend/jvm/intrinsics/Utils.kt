package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Type

// Type.CHAR_TYPE -> "Char"
fun getKotlinPrimitiveClassName(type: Type): Name {
    return JvmPrimitiveType.get(type.className).primitiveType.typeName
}

// "Char" -> type for kotlin.collections.CharIterator
fun getPrimitiveIteratorType(primitiveClassName: Name): Type {
    val iteratorName = Name.identifier(primitiveClassName.asString() + "Iterator")
    return Type.getObjectType(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME.child(iteratorName).internalNameWithoutInnerClasses)
}