// !DIAGNOSTICS: -UNUSED_PARAMETER

// DONT_RUN_GENERATED_CODE: JS

tailrec infix fun Int.foo(x: Int) {
    if (x == 0) return
    val xx = x - 1
    return 1 foo xx
}

fun box(): String {
    1 foo 1000000
    return "OK"
}