fun simpleLess() {
    var a = 1
    if(a < 2) {
        a = 3
    }
    else {
        val b = a
    }
    42
}

fun unknownLess(a: Int, c: Int) {
    var b = 1
    if(b < a) {
        b = a
    }
    if(c < b) {
        b = c
    }
    42
}

fun complexLess(cond1: Boolean, cond2: Boolean) {
    var a = 1
    if(cond1) {
        a = 3
    }
    else if(cond2) {
        a = 5
    }
    if(a < 4) {
        val b = a
    }
    else {
        a = 7
    }
}

fun falseAlarmOnAlwaysTrue() {
    var a = 1
    if(a < 2) {
        a = 3
    }
    42
}