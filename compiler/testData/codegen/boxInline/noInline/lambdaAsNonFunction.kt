// FILE: 1.kt

inline fun test(p: Any) {
    p.toString()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
fun box() : String {
    test {"123"}

    return "OK"
}
