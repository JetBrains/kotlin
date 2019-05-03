/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 3 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: 'When' without bound value and with invalid list of the boolean conditions in 'when entry'.
 */

fun case_1() {
    when {
        value == 21, -> return ""
        value is Int, value is String, -> return ""
        value in -100..100, value in value, -> return ""
    }
    when {
        value == 21, , -> return ""
        value is Int, ,value is String -> return ""
        value in -100..100, ,value in value -> return ""
    }
    when {
        , , -> return ""
    }
    when {
        , value == 21 -> return ""
        , value is Int, value is String -> return ""
        , value in -100..100, value in value -> return ""
    }
    when {
        value is Int value is String -> return ""
        value in -100..100 value in value -> return ""
    }

    return ""
}
