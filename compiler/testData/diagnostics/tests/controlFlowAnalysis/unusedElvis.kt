fun foo(a: Any?) {
    a ?: ""
}

fun foo1(a: Any?) {
    a ?: sideEffect()
}

fun sideEffect() {}