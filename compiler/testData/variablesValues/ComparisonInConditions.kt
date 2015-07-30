fun simpleLt() {
    val a = 1
    if (a < 2) {
        42
    }
    else {
        43
    }
    if (a < 1) {
        44
    }
    else {
        45
    }
    46
}

fun simpleGt() {
    val a = 1
    if (a > 0) {
        42
    }
    else {
        43
    }
    if (a > 1) {
        44
    }
    else {
        45
    }
    46
}

fun simpleLq() {
    val a = 1
    if (a <= 0) {
        42
    }
    else {
        43
    }
    if (a <= 1) {
        44
    }
    else {
        45
    }
    46
}

fun simpleGq() {
    val a = 1
    if (a >= 2) {
        42
    }
    else {
        43
    }
    if (a >= 1) {
        44
    }
    else {
        45
    }
    46
}

fun simpleLess() {
    var a = 1
    if (a < 2) {
        a = 3
    }
    else {
        val b = a
    }
    42
}

fun unknownLess(a: Int, c: Int) {
    var b = 1
    if (b < a) {
        b = a
    }
    if (c < b) {
        b = c
    }
    42
}

fun complexLess(cond1: Boolean, cond2: Boolean) {
    var a = 1
    if (cond1) {
        a = 3
    }
    else if (cond2) {
        a = 5
    }
    if (a < 4) {
        val b = a
    }
    else {
        a = 7
    }
}

fun falseAlarmOnAlwaysTrue() {
    var a = 1
    if (a < 2) {
        a = 3
    }
    42
}

fun multipleElseIf(args: Array<Int>) {
    var a = 1
    if (args.size() == 999) {
        a = 2
    }
    else if (args.size() < 888) {
        a = 3
    }
    else if (args.size() < 777) {
        a = 4
    }
    if (a < 2) {
        42
    }
    else if (a > 3) {
        43
    }
    else if (a < 7) {
        44
    }
    else {
        45
    }
    46
}