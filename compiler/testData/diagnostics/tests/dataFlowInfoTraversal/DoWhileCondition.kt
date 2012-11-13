fun simpleDoWhile(x: Int?, var y: Int) {
    do {
        x : Int?
        y++
    } while (x!! == y)
    x : Int
}

fun doWhileWithBreak(x: Int?, var y: Int) {
    do {
        x : Int?
        y++
        if (y > 0) break
    } while (x!! == y)
    <!TYPE_MISMATCH!>x<!> : Int
}
