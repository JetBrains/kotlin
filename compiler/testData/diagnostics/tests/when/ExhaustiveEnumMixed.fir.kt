/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 5
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 */

enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyEnum.A -> 1
        is <!UNRESOLVED_REFERENCE!>MyEnum.B<!> -> 2
        is <!UNRESOLVED_REFERENCE!>MyEnum.C<!> -> 3
    }
}
