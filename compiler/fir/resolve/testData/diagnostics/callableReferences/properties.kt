class A {
    val bar: Int = 1
}

val bar = 1

val A.baz: Int get() = 1

fun foo1(x: () -> Int) {}
fun foo2(x: (A) -> Int) {}

fun <R> foo3(x: () -> R) {}
fun <T, R> foo4(x: (T) -> R) {}

fun main() {
    foo1(::bar)
    foo2(A::bar)
    foo2(A::baz)

    foo3(::bar)
    foo4(A::bar)
    foo4(A::baz)
}
