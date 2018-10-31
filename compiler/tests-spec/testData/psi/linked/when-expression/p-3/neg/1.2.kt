/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 3
 * SENTENCE: [1] When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 * NUMBER: 2
 * DESCRIPTION: 'When' without bound value and missed 'when condition'.
 */

fun case_1() {
    when {
        -> { println(1) }
    }
}