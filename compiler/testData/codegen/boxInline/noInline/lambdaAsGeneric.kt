// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

var s = ""

inline fun <T> test1(p: T) {
    s += "A"
    p.toString()
}

inline fun <reified T> test2(p: T) {
    s += "B"
    p.toString()
}


// FILE: 2.kt

fun box() : String {
    test1 { "123" }
    test2 { "123" }
    if (s != "AB") return "FAIL: $s"
    return "OK"
}
