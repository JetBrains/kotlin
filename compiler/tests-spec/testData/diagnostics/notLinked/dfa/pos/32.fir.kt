// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun <T: Any, K: Any> case_1(x: T?, y: K?) {
    x as T
    y as K
    val z = <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!> <!USELESS_ELVIS!>?: <!DEBUG_INFO_EXPRESSION_TYPE("K? & K?!!")!>y<!><!>

    <!DEBUG_INFO_EXPRESSION_TYPE("T? & T?!!")!>x<!>.equals(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>z<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>z<!>.equals(10)
}

// TESTCASE NUMBER: 1
inline fun <reified T: Any, reified K: T> case_2(y: K?) {
    y as K

    <!DEBUG_INFO_EXPRESSION_TYPE("K? & K?!!")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("K? & K?!!")!>y<!>.equals(10)
}
