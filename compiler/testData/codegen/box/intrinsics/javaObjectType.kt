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

inline fun <reified T : Any> getJavaObjectType3():Class<*> {
    return T::class.javaObjectType
}

fun getJavaObjectType4():Class<*> {
    return A()::class.javaObjectType
}

fun getJavaObjectType5():Class<*> {
    val i = Integer(10)
    return i::class.javaObjectType
}

fun getJavaObjectType6():Class<*> {
    val i = 10
    return i::class.javaObjectType
}

fun getJavaObjectType7():Class<*> {
    return m()::class.javaObjectType
}

fun getJavaObjectType8():Class<*> {
    return Void::class.javaObjectType
}

fun getJavaObjectType9():Class<*> {
    return Boolean::class.javaObjectType
}

fun box(): String {
    if (getJavaObjectType1() !== Int::class.javaObjectType) {
        return "Failure 1"
    }
    if (getJavaObjectType2() !== Int::class.javaObjectType) {
        return "Failure 2"
    }
    if (getJavaObjectType3<A>() !== A::class.javaObjectType) {
        return "Failure 3 (A)"
    }
    if (getJavaObjectType3<Int>() !== Int::class.javaObjectType) {
        return "Failure 3 (Int)"
    }
    if (getJavaObjectType3<Integer>() !== Int::class.javaObjectType) {
        return "Failure 3 (Integer)"
    }
    if (getJavaObjectType4() !== A::class.javaObjectType) {
        return "Failure 4"
    }
    if (getJavaObjectType5() !== Int::class.javaObjectType) {
        return "Failure 5"
    }
    if (getJavaObjectType6() !== Int::class.javaObjectType) {
        return "Failure 6"
    }
    if (getJavaObjectType7() !== Unit::class.javaObjectType) {
        return "Failure 7"
    }
    if (getJavaObjectType8() !== Void::class.javaObjectType) {
        return "Failure 8"
    }
    if (getJavaObjectType9() !== java.lang.Boolean::class.javaObjectType) {
        return "Failure 9"
    }
    var x = 42
    if ({ x *= 2; x }()::class.javaObjectType != Int::class.javaObjectType || x != 84) {
        return "Failure 10"
    }

    return "OK"
}