fun simpleDoWhile(x: Int?, y0: Int) {
    var y = y0
    do {
        x : Int?
        y++
    } while (x!! == y)
    x : Int
}

fun doWhileWithBreak(x: Int?, y0: Int) {
    var y = y0
    do {
        x : Int?
        y++
        if (y > 0) break
    } while (x!! == y)
    <!TYPE_MISMATCH!>x<!> : Int
}
