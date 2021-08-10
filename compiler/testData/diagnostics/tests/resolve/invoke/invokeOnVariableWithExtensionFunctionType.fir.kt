// FILE: 1.kt
package fooIsExtension

class A
class B

val A.foo: B.() -> Unit get() = {}

fun test(a: A, b: B) {
    b.(<!UNRESOLVED_REFERENCE!>a.foo<!>)()
    (a.foo)(b)
    a.foo(b)

    with(a) {
        b.foo()

        b.(foo)()

        <!ARGUMENT_TYPE_MISMATCH!>(b.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>)()<!>

        foo(b)
        (foo)(b)
    }

    with(b) {
        a.foo(<!NO_VALUE_FOR_PARAMETER!>)<!>
        a.(foo)(<!NO_VALUE_FOR_PARAMETER!>)<!>

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

// FILE: 2.kt
package fooIsMember

class A {
    val foo: B.() -> Unit get() = {}
}
class B

fun test(a: A, b: B) {
    b.(<!UNRESOLVED_REFERENCE!>a.foo<!>)()
    (a.foo)(b)
    a.foo(b)

    with(a) {
        b.foo()

        b.(foo)()

        (<!UNRESOLVED_REFERENCE!>b.<!FUNCTION_CALL_EXPECTED!>foo<!><!>)()

        foo(b)
        (foo)(b)
    }

    with(b) {
        a.foo(<!NO_VALUE_FOR_PARAMETER!>)<!>
        a.(foo)(<!NO_VALUE_FOR_PARAMETER!>)<!>

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
