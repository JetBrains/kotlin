// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

inline fun <reified T : Any> check(expected: String) {
    val clazz = T::class.java!!
    assert (clazz.canonicalName == "java.lang.$expected") {
        "clazz name: ${clazz.canonicalName}"
    }
}

fun box(): String {
    check<Boolean>("Boolean")
    check<Char>("Character")
    check<Byte>("Byte")
    check<Short>("Short")
    check<Int>("Integer")
    check<Float>("Float")
    check<Long>("Long")
    check<Double>("Double")

    check<String>("String")
    check<Void>("Void")

    return "OK"
}
