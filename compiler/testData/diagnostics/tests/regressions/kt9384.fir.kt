// !DIAGNOSTICS: -UNUSED_PARAMETER
fun main(args: Array<String>) {
    fun f() = run {
        <!WRONG_MODIFIER_TARGET!>private<!> class C {
            private fun foo() {
                f().<!UNRESOLVED_REFERENCE!>foo<!>();

            }
        }
        C()
    }
}