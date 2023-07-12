fun foo() {
    fun bar1() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar1()<!>

    fun bar2() = 1 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar2()<!>
    fun bar3() = id(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar3()<!>)
}

fun <T> id(x: T) = x
