// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: B.java

public abstract class B<T> implements A<T> {}

// FILE: test.kt

interface A<T> {
    val content: T
}
fun f(x: Any?) {}
fun f(x: Byte) {}
fun f(x: Char) {}

fun g(i: Int) {}

fun g(x: B<Int>) {
    val y = x.content
    if (y == null) {
        f(y)
        g(y)
    }

    if (y is Nothing?) {
        <!AMBIGUITY!>f<!>(y)
        <!AMBIGUITY!>g<!>(y)
    }
}