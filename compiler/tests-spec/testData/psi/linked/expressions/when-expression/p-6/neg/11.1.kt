/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 6 -> sentence 11
 * NUMBER: 1
 * DESCRIPTION: 'When' with invalid else condition.
 */

fun case_1() {
    when (value) {
        else ->
    }
    when (value) {
        else ->
        else ->
    }
    when (value) {
        1 -> println("1")
        2 -> println("2")
        else ->
    }
    when (value) {
        1 -> println("!")
        else ->
    }
}