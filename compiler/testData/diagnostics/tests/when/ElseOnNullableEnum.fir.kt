// KT-2902 Check for null should be required when match nullable enum element
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 10
 */

// FILE: 1.kt

enum class E { A, B }

fun test(e: E?) = <!NO_ELSE_IN_WHEN!>when<!> (e) {
    E.A -> 1
    E.B -> 2
}

fun withNull(e: E?) = when (e) {
    E.A -> 3
    E.B -> 4
    null -> null
}

fun nullableNothing(): Nothing? = null
fun withNullableNothing(e: E?) = when (e) {
    E.A -> 5
    E.B -> 6
    nullableNothing() -> null
}

fun platformType() = when (J.foo()) {
    E.A -> 7
    E.B -> 8
}

fun platformTypeSmartCast(): Int {
    val e = J.foo()
    if (e == null) return -1
    return when (e) {
        E.A -> 1
        E.B -> 2
    }
}


// FILE: J.java

class J {
    static E foo() {
        return E.A;
    }
}
