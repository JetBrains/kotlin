class KotlinClass {
    companion object {
        const val FOO_INT: Int = 10
        @JvmField val FOO_STRING: String = "OK"
    }
}

fun box(): String {
    val test = JavaClass().test()
    return if (test == "OK10") "OK" else "fail : $test"
}
