/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 5
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 * control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 1
 * control--and-data-flow-analysis, performing-analysis-on-the-control-flow-graph, variable-initialization-analysis -> paragraph 2 -> sentence 3
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    val res: Int
    // See KT-6046: res is always initialized
    when (dir) {
        Direction.NORTH -> res = 1
        Direction.SOUTH -> res = 2
        Direction.WEST  -> res = 3
        Direction.EAST  -> res = 4
    }
    return res
}
