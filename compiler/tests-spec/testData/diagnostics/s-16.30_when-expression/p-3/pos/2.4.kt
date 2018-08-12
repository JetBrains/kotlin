// !WITH_BASIC_TYPES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 4
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (boolean literals and return boolean values).
 */

// CASE DESCRIPTION: 'When' without 'else' branch.
fun case_1(value1: _BasicTypesProvider): String {
    when {
        false || false && true || ((((true)))) -> return ""
        ((value1.getBoolean("1"))) || true -> return ""
        value1.getBoolean("2") && ((((false)))) -> return ""
        value1.getBoolean("3") || !!!!!!getBoolean("4") && getBoolean("5") -> return ""
        value1.getBoolean("6") -> return ""
        true && true && true && !!!true && true -> return ""
        false || false || false || !!!false || false -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch.
fun case_2(value1: _BasicTypesProvider): String {
    return when {
        false || false && true || ((((true)))) -> return ""
        ((value1.getBoolean("1"))) || true -> return ""
        value1.getBoolean("2") && ((((false)))) -> return ""
        value1.getBoolean("3") || !!!!!!getBoolean("4") && getBoolean("5") -> return ""
        value1.getBoolean("6") -> return ""
        true && true && true && !!!true && true -> return ""
        false || false || false || !!!false || false -> return ""
        else -> ""
    }
}