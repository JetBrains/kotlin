/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 6
 * SENTENCE: [1] When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 * NUMBER: 3
 * DESCRIPTION: 'When' with bound value and with invalid list of the conditions in 'when entry'.
 */

fun case_1() {
    when (value) {
        -10000, value.getInt(11), Int.MIN_VALUE, -> return ""
        21, , -> return ""
        , , -> return ""
        , value.getInt(11) -> return ""
        value.getInt(11) Int.MIN_VALUE -> return ""
        value.getInt(11) 200 -> return ""
    }

    return ""
}
