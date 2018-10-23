// !CHECK_TYPE
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 1
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via else branch).
 */

// CASE DESCRIPTION: Checking correctness type (custom types) in 'when' without bound value.
fun case_1(value_1: Int): String {
    val whenValue = when {
        value_1 == 0 -> _ClassLevel2()
        value_1 > 0 && value_1 <= 10 -> _ClassLevel3()
        value_1 > 10 && value_1 <= 100 -> _ClassLevel4()
        else -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correctness type (custom types) in 'when' with bound value.
fun case_2(value_1: Int): String {
    val whenValue = when (value_1) {
        0 -> _ClassLevel2()
        1 -> _ClassLevel3()
        2 -> _ClassLevel4()
        else -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

/*
 CASE DESCRIPTION: Checking correctness type (numbers) in 'when' without bound value.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-25268
 */
fun case_3(value_1: Int): String {
    val whenValue = when {
        value_1 == 0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        value_1 == 1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        value_1 == 2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        value_1 == 3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        value_1 == 4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
    }

    whenValue checkType { _<Any>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Number>() } // unexpected behaviour!
    checkSubtype<Number>(<!TYPE_MISMATCH!>whenValue<!>) // unexpected behaviour!

    return ""
}

/*
 CASE DESCRIPTION: Checking correctness type (numbers) in 'when' with bound value.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-25268
 */
fun case_4(value_1: Int): String {
    val whenValue = when (value_1) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toByte()<!>
    }

    whenValue checkType { _<Any>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Number>() } // unexpected behaviour!
    checkSubtype<Number>(<!TYPE_MISMATCH!>whenValue<!>) // unexpected behaviour!

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' without bound value.
fun case_5(value_1: Int): String {
    val whenValue = when {
        value_1 == 0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        value_1 > 0 && value_1 <= 10 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        value_1 > 10 && value_1 <= 100 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with bound value.
fun case_6(value_1: Int): String {
    val whenValue = when (value_1) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        2 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}
