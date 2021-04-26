// FIR_IDENTICAL
data class A<T>(val i: T)

fun <T> foo(block: (A<T>) -> Unit) {}

fun <T, R> bar() {
    foo<R> { (<!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>i: T<!>) ->
        i
    }
}

data class C<T>(val x: Int, val y: T)

fun <T, S> foo(c: C<T>) {
    val (x: Int, y: S) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>c<!>
}
