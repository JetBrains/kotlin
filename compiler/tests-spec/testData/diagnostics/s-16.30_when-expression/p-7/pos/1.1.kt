/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 1: Type test condition: type checking operator followed by type.
 NUMBER: 1
 DESCRIPTION: 'When' with bound value and type test condition.
 */

// CASE DESCRIPTION: 'When' with type test condition on the various basic types.
fun case_1(value: Any): String {
    when (value) {
        is Int -> return ""
        is Float -> return ""
        is Double -> return ""
        is String -> return ""
        is Char -> return ""
        is Boolean -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the various basic types.
fun case_2(value: Any): String = when (value) {
    is Int -> ""
    is Float -> ""
    is Double -> ""
    is String -> ""
    is Char -> ""
    is Boolean -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the one basic types (Int).
fun case_3(value: Any): String = when (value) {
    is Int -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the one basic types (Int).
fun case_4(value: Any): String {
    when (value) {
        is Int -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on Any.
fun case_5(value: Any): String = when (value) {
    <!USELESS_IS_CHECK!>is Any<!> -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on Nothing.
fun case_6(value: Any): String = when (value) {
    is Nothing -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on Unit.
fun case_7(value: Any): String = when (value) {
    is Unit -> ""
    else -> ""
}