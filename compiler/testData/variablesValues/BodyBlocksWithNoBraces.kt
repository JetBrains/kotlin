fun onlyIfElseNoElse(cond1: Boolean, cond2: Boolean) {
    var a = 1
    if(cond1)
        a = 2
    else if(cond2)
        a = 3
    42
}

fun alwaysFalseIf() {
    var a = 1
    if (a < 0)
        a = 10000
    42
}

fun alwaysTrueIfWithElse() {
    var a = 5
    if (a > 2)
        a = 2
    else
        a = 5
    42
}