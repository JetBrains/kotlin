// !CHECK_TYPE
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 3
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via boolean bound value).
 */

// CASE DESCRIPTION: Checking all types except the correct one in 'when'.
fun case_1(value: Boolean): String {
    val whenValue = when (value) {
        true -> _ClassLevel2()
        false -> _ClassLevel3()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the correct one in 'when' with null-check branch.
fun case_2(value: Boolean?): String {
    val whenValue = when (value) {
        true -> _ClassLevel2()
        false -> _ClassLevel3()
        null -> _ClassLevel4()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when'.
fun case_3(value: Boolean): String {
    val whenValue = when (value) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> <!IMPLICIT_CAST_TO_ANY!>""<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: Boolean?): String {
    val whenValue = when (value) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        null -> <!IMPLICIT_CAST_TO_ANY!>""<!>
    }


    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}


