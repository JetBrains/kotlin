// FIR_IDENTICAL
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 10
 *  - expressions, when-expression -> paragraph 9 -> sentence 2
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 1
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 3
 */

// Base for KT-6227
enum class X { A, B, C, D }

fun foo(arg: X?): String {
    var res = "XXX"
    // Should we report something here? Probably not, null is not an enum entry
    when (arg) {
        X.A -> res = "A"
        X.B -> res = "B"
        X.C -> res = "C"
        X.D -> res = "D"
    }
    return res
}