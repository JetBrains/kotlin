fun oneVariable() {
    val a = 1
    42
}

fun twoVariables() {
    val a = 1
    val b: Int
    b = 2
    42
}

fun moreVariables() {
    val a: Int
    var b = 1
    var c: Int
    a = 2
    c = 3
    c = 4
}

fun unknownVariables(a: Int) {
    val s = ""
    var b = s.length()
    42
}