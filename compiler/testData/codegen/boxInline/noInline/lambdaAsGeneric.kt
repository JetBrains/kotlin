// FILE: 1.kt

inline fun <T> test(p: T) {
    p.toString()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
fun box() : String {
    test {"123"}

    return "OK"
}
