// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: expressions, when-expression -> paragraph 9 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via boolean bound value).
 * HELPERS: classes, checkType
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean): String {
    val whenValue = when (value_1) {
        true -> ClassLevel2()
        false -> ClassLevel3()
    }

    whenValue checkType { <!TYPE_MISMATCH!>check<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>check<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>check<!><ClassLevel1>() }
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

    whenValue checkType { <!TYPE_MISMATCH!>check<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>check<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>check<!><ClassLevel1>() }
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

    whenValue checkType { <!TYPE_MISMATCH!>check<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>check<!><String>() }
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


    whenValue checkType { <!TYPE_MISMATCH!>check<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>check<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>check<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}
