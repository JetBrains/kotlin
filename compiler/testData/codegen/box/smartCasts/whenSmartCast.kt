// IGNORE_BACKEND_FIR: JVM_IR
fun baz(s: String?): Int {
    if (s == null) return 0
    return when(s) {
        "abc" -> s
        else -> "xyz"
    }.length
}

fun box() = if (baz("abc") == 3 && baz("") == 3 && baz(null) == 0) "OK" else "FAIL"
