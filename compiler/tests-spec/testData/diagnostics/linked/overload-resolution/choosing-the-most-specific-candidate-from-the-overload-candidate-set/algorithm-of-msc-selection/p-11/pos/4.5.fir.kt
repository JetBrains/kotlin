// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 11 -> sentence 4
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 3
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 11 -> sentence 4
 * built-in-types-and-their-semantics, built-in-integer-types-1, integer-type-widening -> paragraph 3 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 3
 * NUMBER: 5
 * DESCRIPTION: call with explicit receiver
 */

// TESTCASE NUMBER: 5
class Case5 {
    fun <T> List<T>.foo(x: T, y: Int = 1, z: Int = 1)  : Unit =TODO()

    fun <T> List<T>.foo(x: T, y: Int = 1) : String =TODO()

    fun case(list: List<Int>) {
        list.<!DEBUG_INFO_CALL("fqName: Case5.foo; typeCall: extension function")!>foo(1)<!>
        list.foo(1)
    }
}
