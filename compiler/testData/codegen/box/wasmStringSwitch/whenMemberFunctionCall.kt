class C(private val value: String) {
    fun foo(): String = value
}

fun box(): String {
    val c = C("OK")

    return when {
        c.foo() == "A" -> "FAIL1"
        c.foo() == "OK" -> "OK"
        else -> "FAIL2"
    }
}
