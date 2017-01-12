infix fun Int.test(x : Int) : Int {
    if (this > 1) {
        return (this - 1) test x
    }
    return this
}

fun box() : String = if (10.test(10) == 1) "OK" else "FAIL"