/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 1
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 3
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 3 -> sentence 2
 */

fun foo(a: Boolean, b: Boolean): Int {
    var x: Int
    if (a) {
        x = 1
    }
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (b) {
        true -> x = 2
        false -> x = 3
    }<!>
    return x
}

fun bar(a: Boolean, b: Boolean): Int {
    var x: Int
    if (a) {
        x = 1
    }
    when (b) {
        false -> x = 3
    }
    return <!UNINITIALIZED_VARIABLE!>x<!>
}