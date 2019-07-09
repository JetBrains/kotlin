// !CHECK_TYPE

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
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

    whenValue checkType { _<ClassLevel2>() }
    checkSubtype<ClassLevel1>(whenValue)

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean?): String {
    val whenValue = when (value_1) {
        true -> ClassLevel2()
        false -> ClassLevel3()
        null -> ClassLevel4()
    }

    whenValue checkType { _<ClassLevel2>() }
    checkSubtype<ClassLevel1>(whenValue)

    return ""
}


// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean): String {
    val whenValue = when (value_1) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}


// TESTCASE NUMBER: 4
fun case_4(value_1: Boolean?): String {
    val whenValue = when (value_1) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        null -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}


