/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 3
 * SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
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
