/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 3
 * SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
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
