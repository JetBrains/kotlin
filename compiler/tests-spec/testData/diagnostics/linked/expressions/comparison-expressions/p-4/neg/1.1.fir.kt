// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, comparison-expressions -> paragraph 4 -> sentence 1
 * PRIMARY LINKS: overloadable-operators -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The compareTo operator function must have return type kotlin.Int
 */

// TESTCASE NUMBER: 1
class Case1(val a: Int)  {
    var isCompared = false
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Case1):Any = run{
        TODO()
    }
}

fun case1() {
    val a3 = Case1(-1)
    val a4 = Case1(-3)

    val x0 = a3 > a4
    val x1 = a3 < a4
    val x2 = a3 >= a4
    val x3 = a3 <= a4
}


// TESTCASE NUMBER: 2
class Case2(val a: Int) {
    var isCompared = false
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Case2): Nothing = run {
        TODO()
    }
}

fun case2() {
    val a3 = Case2(-1)
    val a4 = Case2(-3)

    val x0 = a3 > a4
    val x1 = a3 < a4
    val x2 = a3 >= a4
    val x3 = a3 <= a4
}


// TESTCASE NUMBER: 3
class Case3(val a: Int) {
    var isCompared = false
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Case3):Long = run{
        TODO()
    }
}

fun case3() {
    val a3 = Case3(-1)
    val a4 = Case3(-3)

    val x0 = a3 > a4
    val x1 = a3 < a4
    val x2 = a3 >= a4
    val x3 = a3 <= a4
}

// TESTCASE NUMBER: 4
class Case4(val a: Int) {
    var isCompared = false
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Case4):Int? = run{
        TODO()
    }
}

fun case4() {
    val a3 = Case4(-1)
    val a4 = Case4(-3)

    val x0 = a3 > a4
    val x1 = a3 < a4
    val x2 = a3 >= a4
    val x3 = a3 <= a4
}
