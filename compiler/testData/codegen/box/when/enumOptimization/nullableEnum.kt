// IGNORE_BACKEND: JS_IR
// CHECK_CASES_COUNT: function=test count=0
// CHECK_IF_COUNT: function=test count=3

enum class E {
    A,
    B
}

fun test(e: E?) = when (e) {
    E.A -> "Fail A"
    null -> "OK"
    E.B -> "Fail B"
}

fun box(): String {
    return test(null)
}
