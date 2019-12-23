class A {
    val foo: B.() -> Unit get() = null!!
}

class B

fun test(a: A, b: B) {
    with(b) {
        a.foo() // here must be error, because a is not extension receiver

        a.foo(this)

        (a.foo)()

        (a.foo)(this)
    }
}
