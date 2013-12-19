// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: NO_EXPLICIT_RECEIVER
// !THIS_OBJECT: A
// !RECEIVER_ARGUMENT: B

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
