/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: 'When' without bound value and with invalid 'else' branch.
 */

fun case_1() {
    when {
        else ->
    }
    when {
        else ->
        else ->
    }
    when {
        value == 1 -> println("1")
        value == 2 -> println("2")
        else ->
    }
    when {
        value == 1 -> println("!")
        else ->
    }
}
