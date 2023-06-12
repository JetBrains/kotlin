// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK:  overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, rationale-1 -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If there are several functions with this property, none of them are the most specific and an overload resolution ambiguity error should be reported by the compiler
 */

// TESTCASE NUMBER: 1
class Case1 {
    fun <T : Number> List<T>.foo(x: T?) {}

    fun <T : Any> List<Any>.foo(x: T) {}

    fun <T : Number> case(x: List<T>, y: T) {
        x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(y)
    }
}