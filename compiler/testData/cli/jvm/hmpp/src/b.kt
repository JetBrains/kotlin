actual class A {
    actual fun foo() {}
    fun actFoo() {}
}

fun acceptB(b: B) {
    b.bar()
}

fun acceptA(a: A) {
    a.foo()
    a.actFoo()
}
