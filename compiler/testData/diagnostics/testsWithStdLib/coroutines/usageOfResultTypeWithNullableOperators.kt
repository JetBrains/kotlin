// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun <T> id(x: T): T = x

private val asFun: () -> Result<Int>? = TODO()
private val Int.intResult: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int>?<!>
    get() = null

fun returnInt(): Int? = 0


fun nullableOperators(r1: Result<Int>?, b: Boolean) {
    if (b) {
        r1<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>!!<!>
        asFun()<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>!!<!>
        returnInt()?.intResult<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>!!<!>.toString()
    }

    if (b) {
        id(r1)<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>!!<!>
    }

    if (b) {
        r1<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?.<!>toString()
        returnInt()?.intResult<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?.<!>toString()
        asFun()<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?.<!>toString()
        id(r1)<!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?.<!>toString()
    }

    if (b) {
        r1 <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> 0
        r1 <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> r1
        asFun() <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> r1 <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> 0
        id(asFun()) <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> 0

        returnInt() ?: returnInt() ?: asFun() <!RESULT_CLASS_WITH_NULLABLE_OPERATOR!>?:<!> 0
    }
}
