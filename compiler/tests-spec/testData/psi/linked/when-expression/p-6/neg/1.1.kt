/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 6
 * SENTENCE: [1] When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and empty control structure body.
 */

fun case_1() {
    when (value) {
        1 ->
    }
}