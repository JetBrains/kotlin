fun foo1(x: Int): Boolean {
    when(x) {
        2 + 2 -> return true
        else -> return false
    }
}

fun foo2(x: Int): Boolean {
    when(x) {
        Int.MAX_VALUE -> return true
        else -> return false
    }
}

fun box(): String {
    if(!foo1(4)) return "FAIL: foo1(4) must be true"
    if(foo1(1)) return "FAIL: foo1(1) must be false"

    if(!foo2(Int.MAX_VALUE)) return "FAIL: foo2(Int.MAX_VALUE) must be true"
    if(foo2(1)) return "FAIL: foo2(1) must be false"

    return "OK"
}
