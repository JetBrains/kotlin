// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS

// WITH_RUNTIME

class A {
}

fun m() {
}

fun getJavaObjectType1():Class<*> {
    return Int::class.javaObjectType
}

fun getJavaObjectType2():Class<*> {
    return Integer::class.javaObjectType
}

fun getJavaObjectType3():Class<*> {
    return Void::class.javaObjectType
}

fun getJavaObjectType4():Class<*> {
    return Boolean::class.javaObjectType
}

fun getJavaObjectType5():Class<*>? {
    return A::class.javaObjectType
}

inline fun <reified T : Any> getJavaObjectType6():Class<*> {
    return T::class.javaObjectType
}

fun getJavaObjectType7():Class<*> {
    return A()::class.javaObjectType
}

fun getJavaObjectType8():Class<*> {
    val i: Int? = 1
    return i!!::class.javaObjectType
}

fun getJavaObjectType9():Class<*> {
    val i: Int = 1
    return i::class.javaObjectType
}

fun getJavaObjectType10():Class<*> {
    return m()::class.javaObjectType
}

fun box(): String {
    if (getJavaObjectType1() !== Int::class.javaObjectType) {
        return "Failure 1"
    }
    if (getJavaObjectType2() !== Int::class.javaObjectType) {
        return "Failure 2"
    }
    if (getJavaObjectType3() !== Void::class.javaObjectType) {
        return "Failure 3"
    }
    if (getJavaObjectType4() !== java.lang.Boolean::class.javaObjectType) {
        return "Failure 4"
    }
    if (getJavaObjectType5() !== A::class.javaObjectType) {
        return "Failure 5"
    }
    if (getJavaObjectType6<A>() !== A::class.javaObjectType) {
        return "Failure 6 (A)"
    }
    if (getJavaObjectType6<Int>() !== Int::class.javaObjectType) {
        return "Failure 6 (Int)"
    }
    if (getJavaObjectType6<Integer>() !== Int::class.javaObjectType) {
        return "Failure 6 (Integer)"
    }
    if (getJavaObjectType7() !== A::class.javaObjectType) {
        return "Failure 7"
    }
    if (getJavaObjectType8() !== Int::class.javaObjectType) {
        return "Failure 8"
    }
    if (getJavaObjectType9() !== Int::class.javaObjectType) {
        return "Failure 9"
    }
    if (getJavaObjectType10() !== Unit::class.javaObjectType) {
        return "Failure 10"
    }
    var x = 42
    if ({ x *= 2; x }()::class.javaObjectType != Int::class.javaObjectType || x != 84) {
        return "Failure 11"
    }

    return "OK"
}
