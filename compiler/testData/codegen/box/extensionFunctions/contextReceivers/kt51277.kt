// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

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