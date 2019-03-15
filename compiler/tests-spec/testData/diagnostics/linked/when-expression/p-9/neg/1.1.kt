// !CHECK_TYPE

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 9 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via else branch).
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String {
    val whenValue = when {
        value_1 == 0 -> ClassLevel2()
        value_1 > 0 && value_1 <= 10 -> ClassLevel3()
        value_1 > 10 && value_1 <= 100 -> ClassLevel4()
        else -> ClassLevel5()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel5>() }
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String {
    val whenValue = when (value_1) {
        0 -> ClassLevel2()
        1 -> ClassLevel3()
        2 -> ClassLevel4()
        else -> ClassLevel5()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel5>() }
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int): String {
    val whenValue = when {
        value_1 == 0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        value_1 == 1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        value_1 == 2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        value_1 == 3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        value_1 == 4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toByte()<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Long>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Double>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Float>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Long>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Double>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Float>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Byte>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Int): String {
    val whenValue = when (value_1) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toByte()<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Long>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Double>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Float>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Long>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Double>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Float>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Byte>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Int): String {
    val whenValue = when {
        value_1 == 0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        value_1 > 0 && value_1 <= 10 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        value_1 > 10 && value_1 <= 100 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Int): String {
    val whenValue = when (value_1) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        2 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}
