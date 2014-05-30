// !CALL: foo

class A {
    fun B.foo() {}
}

trait B

fun bar(a: A, b: B) {
    with (a) {
        with (b) {
            foo()
        }
    }
}

fun <T, R> with(receiver: T, f: T.() -> R) : R = receiver.f()
