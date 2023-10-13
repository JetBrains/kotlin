fun foo(x: Int, vararg y: String) {}
fun bar(x: Int, vararg y: String, z: Boolean) {}

fun test() {
    foo(1)
    foo(1, "")
    foo(1, "my", "yours")
    foo(1, *arrayOf("my", "yours"))

    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    foo(1, <!ARGUMENT_TYPE_MISMATCH!>2<!>)

    bar(1, z = true, y = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>arrayOf("my", "yours")<!>)

    bar(0, z = false, y = <!ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>""<!>, <!ARGUMENT_PASSED_TWICE!>y<!> = "other")
    bar(0, "", <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>true<!>)<!>
    bar(0, z = false, y = <!ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>""<!>, <!ARGUMENT_PASSED_TWICE!>y<!> = "other", <!ARGUMENT_PASSED_TWICE!>y<!> = "yet other")
}
