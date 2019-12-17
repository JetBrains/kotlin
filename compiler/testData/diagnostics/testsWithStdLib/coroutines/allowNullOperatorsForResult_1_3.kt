// !LANGUAGE: -AllowNullOperatorsForResult
// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun test(r: Result<Int>?) {
    r <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> 0
    r<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?.<!>isFailure
}