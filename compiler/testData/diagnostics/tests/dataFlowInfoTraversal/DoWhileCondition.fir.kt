// CHECK_TYPE

fun simpleDoWhile(x: Int?, y0: Int) {
    var y = y0
    do {
        checkSubtype<Int?>(x)
        y++
    } while (x!! == y)
    checkSubtype<Int>(x)
}

fun doWhileWithBreak(x: Int?, y0: Int) {
    var y = y0
    do {
        checkSubtype<Int?>(x)
        y++
        if (y > 0) break
    } while (x!! == y)
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}
