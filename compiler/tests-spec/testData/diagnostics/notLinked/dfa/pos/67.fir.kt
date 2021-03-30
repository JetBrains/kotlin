// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    when (x) {
        is Any -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.equals(10)
        }
        else -> return
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 2
inline fun <reified T : Any> case_2(x: Any?) {
    when (x) {
        is T -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>.equals(10)
        }
        else -> return
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 3
inline fun <K : Any, reified T : K> case_3(x: Any?) {
    if (x is T) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>.equals(10)
    } else throw Exception()

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>.equals(10)
}
