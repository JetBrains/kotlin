/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [7] The else condition, which works the exact same way as it would in the form without bound expression.
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