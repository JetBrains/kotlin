// LANGUAGE: +ContextParameters
// IGNORE_BACKEND: ANDROID

// FILE: lib.kt
class A

class Example {
    context(a: A)
    inline fun fn(x: Int) {}
}

// FILE: main.kt
fun test() {
    with(A()) {
        Example().fn(1)
    }
}

fun box(): String {
    test()
    return "OK"
}
