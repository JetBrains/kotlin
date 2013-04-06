val p = { "OK" }()

val getter: String
    get() = { "OK" }()

fun f() = { "OK" }()

val obj = object : Function0<String>() {
    override fun invoke() = "OK"
}

fun box(): String {
    if (p != "OK") return "FAIL"
    if (getter != "OK") return "FAIL"
    if (f() != "OK") return "FAIL"
    if (obj() != "OK") return "FAIL"

    return "OK"
}
