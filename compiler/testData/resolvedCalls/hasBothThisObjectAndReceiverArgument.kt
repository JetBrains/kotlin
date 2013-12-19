// !CALL: foo
// !EXPLICIT_RECEIVER_KIND: RECEIVER_ARGUMENT
// !THIS_OBJECT: A
// !RECEIVER_ARGUMENT: b

class A {
    fun B.foo() {}
}

trait B

fun bar(a: A, b: B) {
    with (a) {
        b.foo()
    }
}

fun <T, R> with(receiver: T, f: T.() -> R) : R = receiver.f()
