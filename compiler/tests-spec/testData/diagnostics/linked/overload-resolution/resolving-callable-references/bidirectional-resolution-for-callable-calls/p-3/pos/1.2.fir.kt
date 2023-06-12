// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-413
 * MAIN LINK: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 2
 * overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 3
 * SECONDARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 4
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 8 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: a callable reference is itself an argument to an overloaded function call
 */

// TESTCASE NUMBER: 1
class Case1 {
    fun boo(y: () -> Int, x: () -> Number): Unit = TODO()
    fun boo(vararg x: () -> Int): String = TODO()

    val x = 1.0
    fun x() = 1

    fun case() {
        this.boo(::x, ::x)
        this.<!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: function")!>boo(::x, ::x)<!>
        this.boo(::x, ::x)
    }
}

// TESTCASE NUMBER: 2
class Case2 {
    fun boo(y: () -> Int, x: () -> Number): Unit = TODO()
    fun boo(vararg x: () -> Int): String = TODO()

    val x = 1
    fun x() = 1.0

    fun case() {
        this.boo(::x, ::x)
        this.<!DEBUG_INFO_CALL("fqName: Case2.boo; typeCall: function")!>boo(::x, ::x)<!>
        this.boo(::x, ::x)
    }
}
