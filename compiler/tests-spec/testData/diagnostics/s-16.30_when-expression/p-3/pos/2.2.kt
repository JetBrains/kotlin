// !WITH_BASIC_TYPES_PROVIDER

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 2
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (String and Char).
 */

class B {
    fun method_1(char: Char): String {
        return char.toString()
    }
    fun method_2(value: Any): Boolean {
        return value is String
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch.
fun case_1(value1: String, value2: Char, value3: B): String {
    when {
        value1.isEmpty() -> return ""
        value1 == "..." || value1 == ":::" -> return ""
        value1.equals("-") -> return ""
        value2 == '_' -> return ""
        value2 > 10.toChar() -> return ""
        value2.equals('+') -> return ""
        getString('a') == "A" || getString('+') == "+" -> return ""
        value3.method_1('-') == "_" || value3.method_1('/') == "\\" -> return ""
        getBoolean("a") || getBoolean('a') -> return ""
        value3.method_2("a") || value3.method_2('a') -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch.
fun case_2(value1: String, value2: Char, value3: B): String {
    return when {
        value1.isEmpty() -> ""
        value1 == "..." || value1 == ":::" -> ""
        value1.equals("-") -> ""
        value2 == '_' -> ""
        value2 > 10.toChar() -> ""
        value2.equals('+') -> ""
        getString('a') == "A" || getString('+') == "+" -> return ""
        value3.method_1('-') == "_" || value3.method_1('/') == "\\" -> return ""
        getBoolean("a") || getBoolean('a') -> return ""
        value3.method_2("a") || value3.method_2('a') -> return ""
        else -> ""
    }
}