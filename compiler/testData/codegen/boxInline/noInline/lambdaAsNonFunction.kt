// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

inline fun test(p: Any) {
    p.toString()
}

// FILE: 2.kt

fun box() : String {
    test {"123"}

    return "OK"
}
