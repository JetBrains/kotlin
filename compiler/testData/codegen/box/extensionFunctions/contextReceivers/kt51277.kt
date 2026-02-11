// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY
// TARGET_BACKEND: JVM_IR
// IGNORE_HEADER_MODE: JVM_IR

class C<T> {
    fun foo() = 1
}

context(C<*>) fun test() = foo()

fun box(): String {
    with(C<Int>()) {
        test()
    }
    return "OK"
}