// ISSUE: KT-66005

inline fun <reified T> foo(v: T) {
    <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!> == Int
    // This is a comparison of companion objects
    Int == Int
}
