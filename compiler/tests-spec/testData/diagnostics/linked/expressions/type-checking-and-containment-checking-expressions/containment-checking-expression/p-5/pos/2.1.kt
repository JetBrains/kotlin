// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE  -UNUSED_PARAMETER -UNUSED_VARIABLE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, type-checking-and-containment-checking-expressions, containment-checking-expression -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Containment-checking expressions always have type kotlin.Boolean.
 * HELPERS: checkType
 */

class A(val a: Set<Any>) {
    operator fun contains(other: Any?): Boolean = run { this.a.contains(other) }

    fun throwException(b: Boolean): A { if (b) throw Exception() else return this }
}

class C() {
    var isEvaluated: Boolean = false
    fun foo(): C {
        this.isEvaluated = true
        return this
    }
}

// TESTCASE NUMBER: 1
fun case1() {
    val b = A(mutableSetOf(1, C(), 3, false, 2, "azaza"))
    val c = C()
    val a = (c in b.throwException(true))
    a checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 2
fun case2() {
    val b = A(mutableSetOf(1, C(), 3, false, 2, "azaza"))
    val a = (null in b)
    a checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 3
fun case3() {
    val b = A(mutableSetOf(1, C(), 3, false, 2, "azaza"))
    val x  = ""
    val a = (C() in b)
    a checkType { check<Boolean>() }
}

// TESTCASE NUMBER: 4
fun case4(nothing: Nothing) {
    val b = A(mutableSetOf(1, C(), 3, false, 2, "azaza"))
    val a = (nothing in b)
    a checkType { check<Boolean>() }
}