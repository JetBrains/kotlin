/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 4
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 5
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 1
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 3
 */

fun foo(b: Boolean): Int {
    val x: Int
    val y: Int
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (b) {
        true -> y = 1
        false -> y = 0
    }<!>
    // x is initialized here
    x = 3
    return x + y
}