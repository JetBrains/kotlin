annotation class Anno(val position: String)

fun foo() {
    class OriginalClass {
        val prop = 0

        @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"dangling $prop"<!>)<!SYNTAX!><!>
    }
}
