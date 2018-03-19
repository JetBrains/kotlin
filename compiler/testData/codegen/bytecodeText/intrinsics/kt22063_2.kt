// WITH_RUNTIME

class A {
}

fun m() {
}

fun getJavaPrimitiveType1():Class<*>? {
    // GETSTATIC java/lang/Integer.TYPE
    return Int::class.javaPrimitiveType
}

fun getJavaPrimitiveType2():Class<*>? {
    // GETSTATIC java/lang/Integer.TYPE
    return Integer::class.javaPrimitiveType
}

inline fun <reified T : Any> getJavaPrimitiveType3():Class<*>? {
    // INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
    // INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
    // INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaPrimitiveType
    return T::class.javaPrimitiveType
}

fun getJavaPrimitiveType4():Class<*>? {
    // ACONST_NULL
    return A()::class.javaPrimitiveType
}

fun getJavaPrimitiveType5():Class<*>? {
    val i = Integer(10)
    // GETSTATIC java/lang/Integer.TYPE
    return i::class.javaPrimitiveType
}

fun getJavaPrimitiveType6():Class<*>? {
    val i = 10
    // GETSTATIC java/lang/Integer.TYPE
    return i::class.javaPrimitiveType
}

fun getJavaPrimitiveType7():Class<*>? {
    // ACONST_NULL
    return m()::class.javaPrimitiveType
}

fun getJavaPrimitiveType8():Class<*>? {
    // GETSTATIC java/lang/Void.TYPE
    return Void::class.javaPrimitiveType
}

fun getJavaPrimitiveType9():Class<*>? {
    // GETSTATIC java/lang/Boolean.TYPE
    return Boolean::class.javaPrimitiveType
}

// 2 ACONST_NULL
// 4 GETSTATIC java/lang/Integer.TYPE
// 1 GETSTATIC java/lang/Void.TYPE
// 1 GETSTATIC java/lang/Boolean.TYPE
// 1 LDC Ljava/lang/Object;.class
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
// 1 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 1 INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaPrimitiveType