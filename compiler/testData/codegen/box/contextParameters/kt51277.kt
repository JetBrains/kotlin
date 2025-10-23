// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

class C<T> {
    fun foo() = 1
}

context(c: C<*>) fun test() = c.foo()

fun box(): String {
    with(C<Int>()) {
        test()
    }
    return "OK"
}