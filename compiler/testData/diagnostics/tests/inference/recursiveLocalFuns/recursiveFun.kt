fun foo() {
    fun bar() = (fun() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>()<!>)
}
