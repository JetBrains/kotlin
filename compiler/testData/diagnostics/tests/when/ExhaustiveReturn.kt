/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-155, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 2 -> sentence 1
 *  - control--and-data-flow-analysis, control-flow-graph, expressions-1, boolean-operators -> paragraph 0 -> sentence 0
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (dir) {
        Direction.NORTH -> return 1
        Direction.SOUTH -> return 2
        Direction.WEST  -> return 3
        Direction.EAST  -> return 4
    }<!>
    // See KT-1882: no return is needed at the end
}