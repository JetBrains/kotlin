inline fun <T, reified K> bar() {}

fun foo() {
    <!INAPPLICABLE_CANDIDATE!>bar<!><<!CANNOT_INFER_PARAMETER_TYPE!>Int<!>>()
}
