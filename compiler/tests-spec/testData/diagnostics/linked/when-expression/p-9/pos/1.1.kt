// !CHECK_TYPE
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 1
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via else branch).
 */

// CASE DESCRIPTION: Checking correctness type (custom types) in 'when' without bound value.
fun case_1(value: Int): String {
    val whenValue = when {
        value == 0 -> _ClassLevel2()
        value > 0 && value <= 10 -> _ClassLevel3()
        value > 10 && value <= 100 -> _ClassLevel4()
        else -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correctness type (custom types) in 'when' with bound value.
fun case_2(value: Int): String {
    val whenValue = when (value) {
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
fun case_3(value: Int): String {
    val whenValue = when {
        value == 0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        value == 1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        value == 2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        value == 3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        value == 4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
    }

    whenValue checkType { _<Any>() }

    return ""
}

/*
 CASE DESCRIPTION: Checking correctness type (numbers) in 'when' with bound value.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-25268
 */
fun case_4(value: Int): String {
    val whenValue = when (value) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toByte()<!>
    }

    whenValue checkType { _<Any>() }

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' without bound value.
fun case_5(value: Int): String {
    val whenValue = when {
        value == 0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        value > 0 && value <= 10 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        value > 10 && value <= 100 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with bound value.
fun case_6(value: Int): String {
    val whenValue = when (value) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        2 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}
