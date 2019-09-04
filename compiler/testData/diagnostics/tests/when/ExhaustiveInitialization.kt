/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-155, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 5
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 1
 *  - control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 3
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    val res: Int
    // See KT-6046: res is always initialized
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (dir) {
        Direction.NORTH -> res = 1
        Direction.SOUTH -> res = 2
        Direction.WEST  -> res = 3
        Direction.EAST  -> res = 4
    }<!>
    return res
}
