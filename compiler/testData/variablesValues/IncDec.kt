fun simpleInc() {
    var a = 0
    a++
    ++a
}

fun simpleDec() {
    var a = 0
    a--
    --a
}

fun incInExpr() {
    var a = 0
    val b = a++
    val c = ++a
}

fun decInExpr() {
    var a = 0
    val b = a--
    val c = --a
}