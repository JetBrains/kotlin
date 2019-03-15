// !CHECK_TYPE

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 9 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via boolean bound value).
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean): String {
    val whenValue = when (value_1) {
        true -> ClassLevel2()
        false -> ClassLevel3()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean?): String {
    val whenValue = when (value_1) {
        true -> ClassLevel2()
        false -> ClassLevel3()
        null -> ClassLevel4()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean): String {
    val whenValue = when (value_1) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> <!IMPLICIT_CAST_TO_ANY!>""<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Boolean?): String {
    val whenValue = when (value_1) {
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
