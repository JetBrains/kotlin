// !DIAGNOSTICS: -UNUSED_PARAMETER
fun main(args: Array<String>) {
    fun f() = run {
        <!WRONG_MODIFIER_TARGET!>private<!> class C {
            private fun foo() {
                <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>f<!>()<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>();

            }
        }
        C()
    }
}