/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 3
 * SENTENCE: [1] When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 * NUMBER: 1
 * DESCRIPTION: 'When' without bound value and missed control structure body.
 */

fun case_1() {
    when {
        value == 1 ->
    }
}