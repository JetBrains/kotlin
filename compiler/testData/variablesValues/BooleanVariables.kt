fun booleanWithIf(args: Array<Int>) {
    var a = 1
    if (args.size() > 0) {
        a = 3
    }
    else if (args.size() > 2) {
        a = 5
    }
    var cond: Boolean
    if (args.size() > 0) {
        cond = a < 2
    }
    else {
        cond = a > 4
    }
    if (cond) {
        42
    }
    else {
        43
    }
}

fun simpleAnd(args: Array<Int>) {
    var a = 1
    if (args.size() > 0) {
        a = 3
    }
    else if (args.size() > 2) {
        a = 5
    }
    if (a > 1 && a < 5) {
        42
    }
    else {
        43
    }
}

fun andWithUndefined1(cond: Boolean) {
    var a = -1
    if (cond) {
        a = 1
    }
    if (cond && a > 0 && a < 2) {
        42
    }
    else {
        43
    }
}

fun andWithUndefined2() {
    var a = 1
    if ("s" != "s" && a < 5) {
        42
    }
    else {
        43
    }
}

fun simpleNot() {
    val a = 1
    if (!(a < 2)) {
        42
    }
    else {
        43
    }
}

fun mixed(args: Array<Int>) {
    var a = 2
    if (args.size() > 11) {
        a = 3
    }
    else if (args.size() > 12) {
        a = 4
    }
    else if (args.size() > 13) {
        a = 5
    }
    if (!(a > 0 && a < 3 || a >= 4 && a < 10)) {
        42
    }
    else {
        43
    }
}