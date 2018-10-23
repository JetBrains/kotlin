// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
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

fun getJavaPrimitiveType3():Class<*>? {
    return Void::class.javaPrimitiveType
}

fun getJavaPrimitiveType4():Class<*>? {
    return Boolean::class.javaPrimitiveType
}

fun getJavaPrimitiveType5():Class<*>? {
    return A::class.javaPrimitiveType
}

inline fun <reified T : Any> getJavaPrimitiveType6():Class<*>? {
    return T::class.javaPrimitiveType
}

fun getJavaPrimitiveType7():Class<*>? {
    return A()::class.javaPrimitiveType
}

fun getJavaPrimitiveType8():Class<*>? {
    val i:Int? = 1
    return i!!::class.javaPrimitiveType
}

fun getJavaPrimitiveType9():Class<*>? {
    val i:Int = 1
    return i::class.javaPrimitiveType
}

fun getJavaPrimitiveType10():Class<*>? {
    return m()::class.javaPrimitiveType
}

fun box(): String {
    if (getJavaPrimitiveType1() !== Int::class.javaPrimitiveType) {
        return "Failure 1"
    }
    if (getJavaPrimitiveType2() !== Int::class.javaPrimitiveType) {
        return "Failure 2"
    }
    if (getJavaPrimitiveType3() !== Void::class.javaPrimitiveType) {
        return "Failure 3"
    }
    if (getJavaPrimitiveType4() !== Boolean::class.javaPrimitiveType) {
        return "Failure 4"
    }
    if (getJavaPrimitiveType5() !== null) {
        return "Failure 5"
    }
    if (getJavaPrimitiveType6<A>() !== null) {
        return "Failure 6 (A)"
    }
    if (getJavaPrimitiveType6<Int>() !== Int::class.javaPrimitiveType) {
        return "Failure 6 (Int)"
    }
    if (getJavaPrimitiveType7() !== null) {
        return "Failure 7"
    }
    if (getJavaPrimitiveType8() !== Int::class.javaPrimitiveType) {
        return "Failure 8"
    }
    if (getJavaPrimitiveType9() !== Int::class.javaPrimitiveType) {
        return "Failure 9"
    }
    if (getJavaPrimitiveType10() !== null) {
        return "Failure 10"
    }
    var x = 42
    if ({ x *= 2; x }()::class.javaPrimitiveType !== Int::class.javaPrimitiveType || x != 84) {
        return "Failure 11"
    }

    return "OK"
}
