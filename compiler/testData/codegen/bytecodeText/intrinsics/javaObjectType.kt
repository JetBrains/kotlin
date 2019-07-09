// IGNORE_BACKEND: JVM_IR
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

fun getJavaObjectType3():Class<*> {
    // LDC Ljava/lang/Void;.class
    return Void::class.javaObjectType
}

fun getJavaObjectType4():Class<*> {
    // LDC Ljava/lang/Boolean;.class
    return Boolean::class.javaObjectType
}

fun getJavaObjectType5():Class<*>? {
    // LDC LA;.class
    return A::class.javaObjectType
}

inline fun <reified T : Any> getJavaObjectType6(): Class<*> {
    // INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
    // LDC Ljava/lang/Object;.class
    return T::class.javaObjectType
}

fun getJavaObjectType7():Class<*> {
    // INVOKEVIRTUAL java/lang/Object.getClass
    return A()::class.javaObjectType
}

fun getJavaObjectType8():Class<*> {
    val i: Int? = 1
    // LDC Ljava/lang/Integer;.class
    return i!!::class.javaObjectType
}

fun getJavaObjectType9():Class<*> {
    val i: Int = 1
    // LDC Ljava/lang/Integer;.class
    return i::class.javaObjectType
}

fun getJavaObjectType10():Class<*> {
    // GETSTATIC kotlin/Unit.INSTANCE
    // INVOKEVIRTUAL java/lang/Object.getClass
    return m()::class.javaObjectType
}

// 4 LDC Ljava/lang/Integer;.class
// 1 LDC Ljava/lang/Object;.class
// 1 LDC Ljava/lang/Void;.class
// 1 LDC Ljava/lang/Boolean;.class
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.reifiedOperationMarker
// 0 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 0 INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaObjectType
