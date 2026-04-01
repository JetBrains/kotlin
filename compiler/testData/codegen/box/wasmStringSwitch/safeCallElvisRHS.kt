data class B(val s: String?)

fun box(): String {
    val b = B("OK")

    return if (b.s ?: "FAIL" == "OK") {
        "OK"
    } else {
        "FAIL"
    }
}
