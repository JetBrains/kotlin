// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 4
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (boolean literals and return boolean values).
 */

fun isShortString(str: String): Boolean {
    return str.toString().length < 3
}

fun isString(value: Any): Boolean {
    return value is String
}

class A {
    fun method_1(str: String): Boolean {
        return str.toString().length >= 4
    }
    fun method_2(value: Any): Boolean {
        return value is String
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch.
fun case_1(value1: A): String {
    when {
        false || false && true || ((((true)))) -> return ""
        ((value1.method_1("some string"))) || true -> return ""
        value1.method_2("") && ((((false)))) -> return ""
        value1.method_2("") || !!!!!!isShortString("++++") && isString("") -> return ""
        value1.method_2("") -> return ""
        true && true && true && !!!true && true -> return ""
        false || false || false || !!!false || false -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch.
fun case_2(value1: A): String {
    return when {
        false || false && true || ((((true)))) -> return ""
        ((value1.method_1("some string"))) || true -> return ""
        value1.method_2("") && ((((false)))) -> return ""
        value1.method_2("") || !!!!!!isShortString("++++") && isString("") -> return ""
        value1.method_2("") -> return ""
        true && true && true && !!!true && true -> return ""
        false || false || false || !!!false || false -> return ""
        else -> ""
    }
}