// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * PLACE: statements, assignments, operator-assignments -> paragraph 2 -> sentence 1
 * RELEVANT PLACES: statements, assignments, operator-assignments -> paragraph 2 -> sentence 2
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 3
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: An operator assignment A+=B
 */


class B(var a: Int) {
    operator fun plus(value: Int): B {
        a= a + value
        return this
    }
    operator fun plusAssign(value: Int): Unit {
        a= a + value
    }
}

// TESTCASE NUMBER: 1
fun case1() {
    val b = B(1)
    b += 1
}