fun alwaysTrueIf() {
    var a = 5
    if (a > 2) {
        a = 2
    }
    42
}

fun alwaysFalseIf() {
    var a = 1
    if (a < 0) {
        a = 10000
    }
    42
}

fun alwaysTrueIfWithElse() {
    var a = 5
    if (a > 2) {
        a = 2
    }
    else {
        a = 5
    }
    42
}

fun andWithDeadCode() {
    val lst = arrayListOf(1, 2, 3)
    lst.size() > 3 && lst[3] > 0
    42
}