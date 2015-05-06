annotation class Ann(vararg val i: Boolean)
fun foo() {
    val bool1 = true

    @Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>bool1<!>) val <!UNUSED_VARIABLE!>a<!> = bool1
}