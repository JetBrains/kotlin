data class A(val f: Int, val s: Int)

data class Pair<F, S>(val first: F, val second: S)

class B(val f: Int, val s: Int)

fun test1() {
    val a = 10
    val x: Any? = null;
    val <!UNUSED_VARIABLE!>c<!>: Int = when (x) {
        match <!NAME_SHADOWING, UNUSED_VARIABLE!>x<!> -> { val <!UNUSED_VARIABLE!>n<!> = 10; 10 }
        match <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(<!NAME_SHADOWING!>a<!>: Int, <!UNUSED_VARIABLE!>b<!>: Int)<!> -> a
        match Pair<*, *>(<!UNUSED_VARIABLE!>c<!>, <!UNUSED_VARIABLE!>d<!>) -> a + <!UNRESOLVED_REFERENCE!>b<!>
        match A(<!NAME_SHADOWING!>a<!>) -> a
        match <!COMPONENT_FUNCTION_MISSING!>B(<!NAME_SHADOWING!>a<!>: Int)<!> -> a
        match <!COMPONENT_FUNCTION_ON_NULLABLE, COMPONENT_FUNCTION_ON_NULLABLE!>Pair<*, *>?(<!NAME_SHADOWING!>a<!>: Int, b: Int)<!> -> a + b
        match <!UNRESOLVED_REFERENCE!>pair<!>(<!NAME_SHADOWING!>a<!>: Int, b: Int) -> a + b
        match Pair<*, *>(<!NAME_SHADOWING!>a<!>: Int, b: Int) if (a > b) -> a + b
        match Pair<*, *>(10, 20) -> a + <!UNRESOLVED_REFERENCE!>b<!>
        match <!NAME_SHADOWING!>a<!>: Int -> a
        else -> 1
    }
}