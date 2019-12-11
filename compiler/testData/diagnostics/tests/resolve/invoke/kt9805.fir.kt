class A {
    val foo: B.() -> Unit get() = null!!
}

class B

fun test(a: A, b: B) {
    with(b) {
        a.<!INAPPLICABLE_CANDIDATE!>foo<!>() // here must be error, because a is not extension receiver

        a.foo(this)

        <!INAPPLICABLE_CANDIDATE!>(a.foo)()<!>

        (a.foo)(this)
    }
}
