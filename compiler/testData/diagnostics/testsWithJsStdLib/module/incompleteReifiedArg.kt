// RUN_PIPELINE_TILL: FRONTEND
inline fun <T, reified K> bar() {}

fun foo() {
    <!CANNOT_INFER_PARAMETER_TYPE!>bar<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>()
}
