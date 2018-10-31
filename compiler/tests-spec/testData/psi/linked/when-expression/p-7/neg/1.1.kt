/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [1] Type test condition: type checking operator followed by type.
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and type test condition, but missed type in 'when condition'.
 */

fun case_1() {
    when (value) {
        is -> return ""
    }
    when (value) {
        is -> return ""
        is -> return ""
    }
}
