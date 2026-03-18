// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: lib.kt
inline fun <reified T : Any> check(expected: String) {
    val clazz = T::class.java!!
    assert (clazz.canonicalName == "java.lang.$expected") {
        "clazz name: ${clazz.canonicalName}"
    }
}

// FILE: main.kt
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
