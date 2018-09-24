// !CHECK_TYPE
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 3
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via boolean bound value).
 */

// CASE DESCRIPTION: Checking correct type in 'when'.
fun case_1(value: Boolean): String {
    val whenValue = when (value) {
        true -> _ClassLevel2()
        false -> _ClassLevel3()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' with null-check branch.
fun case_2(value: Boolean?): String {
    val whenValue = when (value) {
        true -> _ClassLevel2()
        false -> _ClassLevel3()
        null -> _ClassLevel4()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}


// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when'.
fun case_3(value: Boolean): String {
    val whenValue = when (value) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}


// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: Boolean?): String {
    val whenValue = when (value) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        null -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}


