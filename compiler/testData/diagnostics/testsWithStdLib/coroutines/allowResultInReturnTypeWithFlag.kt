// !ALLOW_RESULT_RETURN_TYPE

fun result(): Result<Int> = TODO()
val resultP: Result<Int> = result()

fun f(r1: Result<Int>?) {
    r1 <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> <!UNUSED_EXPRESSION!>0<!>
}