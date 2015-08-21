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

fun whileWithDeadCode() {
    var a = 2
    while (false) {
        42
    }
    while (a < 0) {
        ++a
    }
    a = 2
    while (a > 0) {
        while (a < 0) {
            ++a
        }
    }
    a = 2
    while (++a <= 2) {
        ++a
    }
    42
}

fun andSequenceWithDeadCode() {
    false && true && true && 1 > 0
}

fun orSequenceWithDeadCode() {
    true || false || true || 1 > 100
}

fun booleanOperatorsSequenceWithDeadCode() {
    false && true || true && true || true || false
}