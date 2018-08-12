// !WITH_BASIC_TYPES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 2
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (String and Char).
 */

// CASE DESCRIPTION: 'When' without 'else' branch.
fun case_1(value1: String, value2: Char, value3: _BasicTypesProvider): String {
    when {
        value1.isEmpty() -> return ""
        value1 == "..." || value1 == ":::" -> return ""
        value1.equals("-") -> return ""
        value2 == '_' -> return ""
        value2 > 10.toChar() -> return ""
        value2.equals('+') -> return ""
        getString('a') == "A" || getString('+') == "+" -> return ""
        value3.getString('-') == "_" || value3.getString('/') == "\\" -> return ""
        getBoolean("a") || getBoolean('a') -> return ""
        value3.getBoolean("a") || value3.getBoolean('a') -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch.
fun case_2(value1: String, value2: Char, value3: _BasicTypesProvider): String {
    return when {
        value1.isEmpty() -> ""
        value1 == "..." || value1 == ":::" -> ""
        value1.equals("-") -> ""
        value2 == '_' -> ""
        value2 > 10.toChar() -> ""
        value2.equals('+') -> ""
        getString('a') == "A" || getString('+') == "+" -> return ""
        value3.getString('-') == "_" || value3.getString('/') == "\\" -> return ""
        getBoolean("a") || getBoolean('a') -> return ""
        value3.getBoolean("a") || value3.getBoolean('a') -> return ""
        else -> ""
    }
}