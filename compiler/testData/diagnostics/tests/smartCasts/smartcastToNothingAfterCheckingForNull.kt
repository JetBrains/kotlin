// DIAGNOSTICS: -UNUSED_PARAMETER

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
        f(<!DEBUG_INFO_CONSTANT!>y<!>)
        <!NONE_APPLICABLE!>g<!>(<!DEBUG_INFO_CONSTANT!>y<!>)
    }

    if (y is Nothing?) {
        f(y)
        <!NONE_APPLICABLE!>g<!>(y)
    }
}