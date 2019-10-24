/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 2 -> sentence 1
 *  - control--and-data-flow-analysis, control-flow-graph, expressions-1, boolean-operators -> paragraph 0 -> sentence 0
 *  - control--and-data-flow-analysis, control-flow-graph, statements-1 -> paragraph 0 -> sentence 0
 */

enum class Color { RED, GREEN, BLUE }

fun foo(arr: Array<Color>): Color {
    loop@ for (color in arr) {
        <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (color) {
            Color.RED -> return color
            Color.GREEN -> break@loop
            Color.BLUE -> if (arr.size == 1) return color else continue@loop
        }<!>
        // Unreachable
        <!UNREACHABLE_CODE!>return Color.BLUE<!>
    }
    return Color.GREEN
}