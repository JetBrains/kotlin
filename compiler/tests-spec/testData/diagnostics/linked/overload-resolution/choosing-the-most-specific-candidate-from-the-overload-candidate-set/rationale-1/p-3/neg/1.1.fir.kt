// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1 {
    fun <T : Number> List<T>.foo(x: T?) {}

    fun <T : Any> List<Any>.foo(x: T) {}

    fun <T : Number> case(x: List<T>, y: T) {
        x.<!AMBIGUITY!>foo<!>(y)
    }
}
