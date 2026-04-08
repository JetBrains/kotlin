// LANGUAGE: +ContextParameters
// IGNORE_BACKEND: ANDROID

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
