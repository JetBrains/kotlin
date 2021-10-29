// NI_EXPECTED_FILE

fun test() = 3

fun <T> proxy(t: T) = t

class A {
    val test = test()
}

class B {
    val test = proxy(test())
}

class C {
    val bar = <!DEBUG_INFO_MISSING_UNRESOLVED!>test<!>()
    val test = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>()<!>
}
