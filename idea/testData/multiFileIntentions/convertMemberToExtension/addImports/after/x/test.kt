package x

interface A {
}

fun A.foo(n: Int) {
    throw UnsupportedOperationException()
}