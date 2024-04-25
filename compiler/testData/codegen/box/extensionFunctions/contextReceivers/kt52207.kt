// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class A

class Example {
    context(A)
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