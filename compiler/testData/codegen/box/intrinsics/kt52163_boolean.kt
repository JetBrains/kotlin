fun test(): Int {
    val d: Any?
    d = true
    return d.compareTo(false)
}

fun box(): String =
    if (test() == 1) "OK" else "Fail"
