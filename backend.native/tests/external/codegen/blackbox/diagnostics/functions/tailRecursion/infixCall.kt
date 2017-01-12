// IGNORE_BACKEND_WITHOUT_CHECK: JS

tailrec infix fun Int.test(x : Int) : Int {
    if (this > 1) {
        return (this - 1) test x
    }
    return this
}

fun box() : String = if (1000000.test(1000000) == 1) "OK" else "FAIL"