// WITH_DEFAULT_VALUE: false
public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class A(val a: Int) {
    fun foo(x: A): Int {
        return (<selection>this.a + x.a</selection>) / 2
    }
}

fun test() {
    A(1).foo(A(2))
    with(A(1)) {
        foo(A(2))
        this.foo(A(2))
    }
}