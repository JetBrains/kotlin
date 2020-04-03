// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, comparison-expressions -> paragraph 5 -> sentence 1
 * RELEVANT PLACES: overloadable-operators -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: All comparison expressions always have type kotlin.Boolean.
 * HELPERS: checkType
 */

class A(val a: Int)  {
    var isCompared = false
    operator fun compareTo(other: A): Int = run {
        isCompared = true
        this.a - other.a
    }
}

// TESTCASE NUMBER: 1
fun case1() {
    val a1 = A(-1)
    val a2 = A(-3)

    val x = a1 < a2

    x checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 2
fun case2() {
    val a1 = A(-1)
    val a2 = A(-3)

    val x = a1 > a2

    x checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 3
fun case3() {
    val a1 = A(-1)
    val a2 = A(-3)

    val x = a1 <= a2

    x checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 4
fun case4() {
    val a1 = A(-1)
    val a2 = A(-3)

    val x = a1 >= a2

    x checkType { check<Boolean>() }
}