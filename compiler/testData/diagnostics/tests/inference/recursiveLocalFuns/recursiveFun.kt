// !WITH_NEW_INFERENCE
fun foo() {
    fun bar() = (fun() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar()<!>)
}
