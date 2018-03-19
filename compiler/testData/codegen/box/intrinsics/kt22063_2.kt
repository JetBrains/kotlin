// IGNORE_BACKEND: JS

// WITH_RUNTIME

class A {
}

fun m() {
}

fun getJavaPrimitiveType1():Class<*>? {
    return Int::class.javaPrimitiveType
}

fun getJavaPrimitiveType2():Class<*>? {
    return Integer::class.javaPrimitiveType
}

inline fun <reified T : Any> getJavaPrimitiveType3():Class<*>? {
    return T::class.javaPrimitiveType
}

fun getJavaPrimitiveType4():Class<*>? {
    return A()::class.javaPrimitiveType
}

fun getJavaPrimitiveType5():Class<*>? {
    val i = Integer(10)
    return i::class.javaPrimitiveType
}

fun getJavaPrimitiveType6():Class<*>? {
    val i = 10
    return i::class.javaPrimitiveType
}

fun getJavaPrimitiveType7():Class<*>? {
    return m()::class.javaPrimitiveType
}

fun getJavaPrimitiveType8():Class<*>? {
    return Void::class.javaPrimitiveType
}

fun getJavaPrimitiveType9():Class<*>? {
    return Boolean::class.javaPrimitiveType
}

fun box(): String {
    if (getJavaPrimitiveType1() !== Int::class.javaPrimitiveType) {
        return "Failure 1"
    }
    if (getJavaPrimitiveType2() !== Int::class.javaPrimitiveType) {
        return "Failure 2"
    }
    if (getJavaPrimitiveType3<A>() !== null) {
        return "Failure 3 (A)"
    }
    if (getJavaPrimitiveType3<Int>() !== Int::class.javaPrimitiveType) {
        return "Failure 3 (Int)"
    }
    if (getJavaPrimitiveType3<Integer>() !== Int::class.javaPrimitiveType) {
        return "Failure 3 (Integer)"
    }
    if (getJavaPrimitiveType4() !== null) {
        return "Failure 4"
    }
    if (getJavaPrimitiveType5() !== Int::class.javaPrimitiveType) {
        return "Failure 5"
    }
    if (getJavaPrimitiveType6() !== Int::class.javaPrimitiveType) {
        return "Failure 6"
    }
    if (getJavaPrimitiveType7() !== null) {
        return "Failure 7"
    }
    println(Void::class.javaPrimitiveType)
    if (getJavaPrimitiveType8() !== Void::class.javaPrimitiveType) {
        return "Failure 8"
    }
    if (getJavaPrimitiveType9() !== Boolean::class.javaPrimitiveType) {
        return "Failure 9"
    }
    return "OK"
}