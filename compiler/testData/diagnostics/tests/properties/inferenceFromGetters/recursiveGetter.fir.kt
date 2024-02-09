// !CHECK_TYPE
// NI_EXPECTED_FILE

val x get() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x<!>

class A {
    val y get() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>y<!>

    val a get() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>b<!>
    val b get() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>a<!>

    val z1 get() = <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>z1<!>)
    val z2 get() = <!CANNOT_INFER_PARAMETER_TYPE!>l<!>(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>z2<!>)

    val u get() = <!UNRESOLVED_REFERENCE!>field<!>
}

fun <E> id(x: E) = x
fun <E> l(x: E): List<E> = null!!
