fun testOr(b: Boolean): Boolean {
    return b || return !b;
}

fun testOr(): Boolean {
    return true || return false;
}


fun testAnd(b: Boolean): Boolean {
    return b && return !b;
}

fun testAnd(): Boolean {
    return true && return false;
}

fun box(): String {
    if (testOr(false) != true) return  "fail 1"
    if (testOr(true) != true) return "fail 2"
    if (testAnd(false) != false) return "fail 3"
    if (testAnd(true) != false) return "fail 4"
    if (testOr() != true) return "fail 5"
    if (testAnd() != false) return "fail 6"
    return "OK"
}