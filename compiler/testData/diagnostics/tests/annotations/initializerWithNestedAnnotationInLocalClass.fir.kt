annotation class Anno(val position: String)

fun foo() {
    class MyClass {
        val prop = 0

        <!WRONG_ANNOTATION_TARGET!>@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"init $prop"<!>)<!>  init {

        }
    }
}
