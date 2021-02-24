/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 9 -> sentence 1
 * expressions, when-expression -> paragraph 2 -> sentence 4
 * expressions, when-expression -> paragraph 2 -> sentence 5
 */

package test

const val four = 4

fun first(arg: Int) = when (arg) {
    1 -> 2
    2 -> 3
    1 -> 4
    4 -> 5
    1 -> 6
    2 -> 7
    // Error should be here: see KT-11971
    four -> 8
    else -> 0
}

fun second(arg: String): Int {
    when (arg) {
        "ABC" -> return 0
        "DEF" -> return 1
        "ABC" -> return -1
        "DEF" -> return -2
    }
    return 42
}

fun third(arg: Any?): Int {
    when (arg) {
        null -> return -1
        is String -> return 0
        is Double -> return 1
        is Double -> return 2
        null -> return 3
        else -> return 5
    }
}

enum class Color { RED, GREEN, BLUE }

fun fourth(arg: Color) = when (arg) {
    Color.RED -> "RED"
    Color.GREEN -> "GREEN"
    Color.RED -> "BLUE"
    Color.BLUE -> "BLUE"
}

fun fifth(arg: Any?) = when (arg) {
    is Any -> "Any"
    else -> ""
    else -> null
}
