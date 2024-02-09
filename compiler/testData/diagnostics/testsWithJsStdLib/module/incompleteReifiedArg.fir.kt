inline fun <T, reified K> bar() {}

fun foo() {
    <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>bar<!><Int>()
}
