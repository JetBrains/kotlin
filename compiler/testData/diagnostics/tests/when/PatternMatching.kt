data class A(val f: Int, val s: Int)

data class Pair<F, S>(val first: F, val second: S)

class B(val f: Int, val s: Int)

data class C<F, S>(val f: F, val s: S)

fun test1() {
    val a = 10
    val x: Any? = null;
    val <!UNUSED_VARIABLE!>c<!>: Int = when (x) {
        is val <!NAME_SHADOWING, UNUSED_VARIABLE!>x<!> -> { val <!UNUSED_VARIABLE!>n<!> = 10; 10 }
        is <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(val <!NAME_SHADOWING!>a<!>: Int, val <!UNUSED_VARIABLE!>b<!>: Int)<!> -> a
        is Pair(val <!UNUSED_VARIABLE!>c<!>, val <!UNUSED_VARIABLE!>d<!>) -> a + <!UNRESOLVED_REFERENCE!>b<!>
        is A(val <!NAME_SHADOWING!>a<!>) -> a
        is <!COMPONENT_FUNCTION_MISSING!>B(val <!NAME_SHADOWING!>a<!>: Int)<!> -> a
        is <!COMPONENT_FUNCTION_ON_NULLABLE, COMPONENT_FUNCTION_ON_NULLABLE!>Pair?(val <!NAME_SHADOWING!>a<!>: Int, val b: Int)<!> -> a + b
        is <!UNRESOLVED_REFERENCE!>pair<!>(val <!NAME_SHADOWING!>a<!>: Int, val b: Int) -> a + b
        is Pair(val <!NAME_SHADOWING!>a<!>: Int, val b: Int) && a > b -> a + b
        is Pair(10, 20) -> a + <!UNRESOLVED_REFERENCE!>b<!>
        is <!NO_TYPE_ARGUMENTS_ON_RHS!>Pair<!> -> 1
        is Pair(_, val <!NAME_SHADOWING, UNUSED_VARIABLE!>a<!>: <!NO_TYPE_ARGUMENTS_ON_RHS!>Pair<!>) -> 1
        is Pair(_, val <!NAME_SHADOWING, UNUSED_VARIABLE!>a<!>: Pair<*, *>) -> 1
        is C<*, *> -> 2
        is val <!NAME_SHADOWING!>a<!>: Int -> a
        else -> 1
    }
}