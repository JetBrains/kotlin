// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 * control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 1
 * control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 3
 * control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 3 -> sentence 2
 * control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 3 -> sentence 3
 */

fun foo(a: Boolean, b: Boolean): Int {
    val x: Int
    if (a) {
        x = 1
    }
    when (b) {
        true -> <!VAL_REASSIGNMENT!>x<!> = 2
        false -> <!VAL_REASSIGNMENT!>x<!> = 3
    }
    return x
}

fun bar(a: Boolean, b: Boolean): Int {
    val x: Int
    if (a) {
        x = 1
    }
    <!NO_ELSE_IN_WHEN!>when<!> (b) {
        false -> <!VAL_REASSIGNMENT!>x<!> = 3
    }
    return <!UNINITIALIZED_VARIABLE!>x<!>
}
