// FIR_IDENTICAL

interface A<T> {
    operator fun getValue(x: Any?, y: Any?): T = TODO()
}

fun <T> foo(): A<T> = TODO()

fun bar1() {
    val x: String by foo()
}

fun bar2() {
    operator fun <F : A<E>, E> A<E>.provideDelegate(x: Any?, y: Any?): F = TODO()

    val x: String by foo()
}