fun <T> foo(a: T, b: T) = a.hashCode() + b.hashCode()

fun <E> E.foo() = hashCode()

fun <E> E.foo(x: E, y: E) = x.hashCode() + y.hashCode()

fun test(): Int {
    fun foo(a: Int, b: Int) = a + b
    return <expr>foo<Int>(1, 2)</expr>
}
