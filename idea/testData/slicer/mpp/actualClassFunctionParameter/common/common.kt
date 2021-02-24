package sample

expect class ExpectClass() {
    fun foo(p: Any)
}

fun f(c: ExpectClass) {
    c.foo(1)
}