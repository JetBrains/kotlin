// IGNORE_BACKEND: JVM_IR
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

fun getJavaPrimitiveType3():Class<*>? {
    // GETSTATIC java/lang/Void.TYPE
    return Void::class.javaPrimitiveType
}

fun getJavaPrimitiveType4():Class<*>? {
    // GETSTATIC java/lang/Boolean.TYPE
    return Boolean::class.javaPrimitiveType
}

fun getJavaPrimitiveType5():Class<*>? {
    // ACONST_NULL
    return A::class.javaPrimitiveType
}

inline fun <reified T : Any> getJavaPrimitiveType6():Class<*>? {
    // INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
    // LDC Ljava/lang/Object;.class
    // INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
    // INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaPrimitiveType
    return T::class.javaPrimitiveType
}

fun getJavaPrimitiveType7():Class<*>? {
    // INVOKEVIRTUAL java/lang/Object.getClass
    // INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
    // INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaPrimitiveType
    return A()::class.javaPrimitiveType
}

fun getJavaPrimitiveType8():Class<*>? {
    val i:Int? = 1
    // GETSTATIC java/lang/Integer.TYPE
    return i!!::class.javaPrimitiveType
}

fun getJavaPrimitiveType9():Class<*>? {
    val i:Int = 1
    // GETSTATIC java/lang/Integer.TYPE
    return i::class.javaPrimitiveType
}

fun getJavaPrimitiveType10():Class<*>? {
    // INVOKEVIRTUAL java/lang/Object.getClass
    // INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
    // INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaPrimitiveType
    return m()::class.javaPrimitiveType
}

// 1 ACONST_NULL
// 4 GETSTATIC java/lang/Integer.TYPE
// 1 GETSTATIC java/lang/Void.TYPE
// 1 GETSTATIC java/lang/Boolean.TYPE
// 1 LDC Ljava/lang/Object;.class
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
// 2 INVOKEVIRTUAL java/lang/Object.getClass
// 3 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 3 INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaPrimitiveType
