/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 1
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 3
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 3 -> sentence 2
 *  - declarations, classifier-declaration, classifier-initialization -> paragraph 6 -> sentence 4
 */

// See KT-5113
enum class E {
    A,
    B
}

class Outer(e: E) {
    private val prop: Int
    init {
        <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when(e ) {
            // When is exhaustive, property is always initialized
            E.A -> prop = 1
            E.B -> prop = 2
        }<!>
    }
}