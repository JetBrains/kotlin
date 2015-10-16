class B

class A {
    operator fun B.invoke() {}
}

val B.a: () -> Int  get() = { 5 }

fun test(a: A, b: B) {
    val <!UNUSED_VARIABLE!>x<!>: Int = b.a()

    b.<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>(a)<!>()

    with(b) {
        val <!UNUSED_VARIABLE!>y<!>: Int = a()
        <!MISSING_RECEIVER!>(a)<!>()
    }
}

fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()