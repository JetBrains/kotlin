// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-220
 * MAIN LINK: expressions, comparison-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overloadable-operators -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: <, >, <= and >=  operators are overloadable
 */

// TESTCASE NUMBER: 1
class A(val a: Int)  {
    fun compareTo(other: A): Int = run {
        this.a - other.a
    }
}

fun case1() {
    val a3 = A(-1)
    val a4 = A(-3)

    val x = (a3 <!OPERATOR_MODIFIER_REQUIRED!>><!> a4)
}
