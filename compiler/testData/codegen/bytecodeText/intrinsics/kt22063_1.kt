// WITH_RUNTIME

class A {
}

fun m() {
}

fun getJavaObjectType1():Class<*> {
    // LDC Ljava/lang/Integer;.class
    return Int::class.javaObjectType
}

fun getJavaObjectType2():Class<*> {
    // LDC Ljava/lang/Integer;.class
    return Integer::class.javaObjectType
}

inline fun <reified T : Any> getJavaObjectType3():Class<*> {
    // INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
    // LDC Ljava/lang/Object;.class
    return T::class.javaObjectType
}

fun getJavaObjectType4():Class<*> {
    // INVOKEVIRTUAL java/lang/Object.getClass
    return A()::class.javaObjectType
}

fun getJavaObjectType5():Class<*> {
    val i = Integer(10)
    // INVOKEVIRTUAL java/lang/Object.getClass
    return i::class.javaObjectType
}

fun getJavaObjectType6():Class<*> {
    val i = 10
    // LDC Ljava/lang/Integer;.class
    return i::class.javaObjectType
}

fun getJavaObjectType7():Class<*> {
    // GETSTATIC kotlin/Unit.INSTANCE
    // INVOKEVIRTUAL java/lang/Object.getClass
    return m()::class.javaObjectType
}

fun getJavaObjectType8():Class<*> {
    // LDC Ljava/lang/VOID;.class
    return Void::class.javaObjectType
}

fun getJavaObjectType9():Class<*> {
    // LDC Ljava/lang/Boolean;.class
    return Boolean::class.javaObjectType
}

// 3 LDC Ljava/lang/Integer;.class
// 1 LDC Ljava/lang/Object;.class
// 1 LDC Ljava/lang/Void;.class
// 1 LDC Ljava/lang/Boolean;.class
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
// 0 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 0 INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaObjectType