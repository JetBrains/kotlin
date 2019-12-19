// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: JavaTest.java

public class JavaTest {
    public static Number[] createNumberArray() { return null; }
}

// FILE: test.kt

fun <K> select(x: K, y: K): K = x

fun <R> foo(f: () -> R): R = f()

fun test(n: Number) {
    val a = select(foo { JavaTest.createNumberArray() }, emptyArray())

    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<(kotlin.Number..kotlin.Number?)>..kotlin.Array<(kotlin.Number..kotlin.Number?)>?)")!>a<!>
}