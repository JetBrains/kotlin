// !WITH_CLASSES
// !WITH_OBJECTS

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
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

// CASE DESCRIPTION: 'When' with type test condition on the various nullable basic types.
fun case_2(value: Any?): String = when (value) {
    is Int? -> "" // if value is null then this branch will be executed
    is Float -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on Any.
fun case_3(value: Any?): String = when (value) {
    is Any -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on nullable (redundant) Any.
fun case_4(value: Any): String = when (value) {
    <!USELESS_IS_CHECK!>is Any<!USELESS_NULLABLE_CHECK!>?<!><!> -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the various nullable basic types (two nullable type check).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_5(value: Any?): String = when (value) {
    is Double -> ""
    is Int? -> "" // if value is null then this branch will be executed
    is String -> ""
    is Float? -> "" // redundant nullable type check
    is Char -> ""
    is Boolean -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the objetcs.
fun case_6(value: Any): String {
    when (value) {
        is _EmptyObject -> return ""
        is _ClassWithCompanionObject.Companion -> return ""
    }

    return ""
}
