/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [3] Contains test condition: containment operator followed by an expression.
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and 'when condition' with range expression, but without contains operator.
 */

fun case_1() {
    when (value) {
        in -> return ""
    }
    when (value) {
        in -> return ""
        in -> return ""
    }
}
