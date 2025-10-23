// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

class A

class Example {
    context(a: A)
    inline fun fn(x: Int) {}
}

fun test() {
    with(A()) {
        Example().fn(1)
    }
}

fun box(): String {
    test()
    return "OK"
}