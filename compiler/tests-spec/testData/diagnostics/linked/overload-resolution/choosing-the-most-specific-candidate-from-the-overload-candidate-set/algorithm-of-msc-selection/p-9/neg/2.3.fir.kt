// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 9 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: receiver of intersection type ambuguity
 */

// TESTCASE NUMBER: 1
interface A1
interface B1

fun case1(x: Any) {
    fun A1.foo(): kotlin.Int = 1  //(1)
    fun B1.foo(): kotlin.Int = 2  //(2)
    fun Any.foo(): kotlin.String = "Any"  //(3)
    x.<!DEBUG_INFO_CALL("fqName: case1.foo; typeCall: extension function")!>foo()<!> // to (3)
    x.foo()
    if (x is B1 && x is A1) {
        x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    }
}
