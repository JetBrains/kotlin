// FILE: 1.kt
package fooIsExtension

class A
class B

val A.foo: B.() -> Unit get() = {}

fun test(a: A, b: B) {
    b.(a.foo)()
    (a.foo)(b)
    a.foo(b)

    with(a) {
        b.<!MISSING_RECEIVER!>foo<!>() // todo

        b.(foo)()

        (b.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>)()

        foo(b)
        (foo)(b)
    }

    with(b) {
        a.<!INVOKE_ON_EXTENSION_FUNCTION_WITH_EXPLICIT_DISPATCH_RECEIVER!>foo<!>()
        a.<!FUNCTION_EXPECTED!>(<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>)<!>()

        (a.foo)()

        (a.foo)(this)
        a.foo(this)
    }

    with(a) {
        with(b) {
            foo()
            (foo)()
        }
    }
}

public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()


// FILE: 1.kt
package fooIsMember

class A {
    val foo: B.() -> Unit get() = {}
}
class B

fun test(a: A, b: B) {
    b.(a.foo)()
    (a.foo)(b)
    a.foo(b)

    with(a) {
        b.foo()

        b.(foo)()

        <!FUNCTION_EXPECTED!>(b.<!FUNCTION_CALL_EXPECTED!>foo<!>)<!>()

        foo(b)
        (foo)(b)
    }

    with(b) {
        a.<!INVOKE_ON_EXTENSION_FUNCTION_WITH_EXPLICIT_DISPATCH_RECEIVER!>foo<!>()
        a.<!FUNCTION_EXPECTED!>(<!UNRESOLVED_REFERENCE!>foo<!>)<!>()

        (a.foo)()

        (a.foo)(this)
        a.foo(this)
    }

    with(a) {
        with(b) {
            foo()
            (foo)()
        }
    }
}

public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()