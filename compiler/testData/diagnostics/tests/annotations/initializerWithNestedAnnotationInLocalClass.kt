annotation class Anno(val position: String)

fun foo() {
    class MyClass {
        val prop = 0

        <!WRONG_ANNOTATION_TARGET!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"init $prop"<!>)<!>  init {

        }
    }
}
