/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 6 -> sentence 1
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
