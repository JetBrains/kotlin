fun simpleIfElse(cond: Boolean) {
    if(cond) {
        val a = 1
    }
    else {
        val b = 2
    }
}

fun ifWithUpdate(cond: Boolean) {
    var a = 1
    if(cond) {
        a = 2
    }
    42
}

fun ifElseWithUpdate(cond: Boolean) {
    var a = 1
    if(cond) {
        a = 2
    }
    else {
        a = 3
    }
    42
}

fun ifWithLocalVariable(cond: Boolean) {
    var a = 1
    if(cond) {
        val b = 9
        a = b
    }
    42
}

fun multipleIfElse(cond1: Boolean, cond2: Boolean) {
    var a = 1
    if(cond1) {
        a = 2
    }
    else if(cond2) {
        a = 3
    }
    else {
        a = 4
    }
    42
}

fun onlyIfElseNoElse(cond1: Boolean, cond2: Boolean) {
    var a = 1
    if(cond1) {
        a = 2
    }
    else if(cond2) {
        a = 3
    }
    42
}