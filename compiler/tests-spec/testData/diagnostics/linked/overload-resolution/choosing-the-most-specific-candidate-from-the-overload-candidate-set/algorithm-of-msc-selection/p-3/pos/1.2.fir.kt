// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 4
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 8 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: If both X_k and Y_k are built-in integer types, a type constraint Widen(X_k) <:Widen(Y_k) is built
 */

// TESTCASE NUMBER: 1
class Case1 {
    fun boo(y: Int, x: Number): Unit = TODO()
    fun boo(vararg x: Int): String = TODO()
    fun case() {
        this.<!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: function")!>boo(1, 1)<!>
        this.boo(1, 1)
    }
}
