// !CHECK_TYPE
// !WITH_ENUM_CLASSES
// !WITH_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 9 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via enum).
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _EnumClass): String {
    val whenValue = when (value_1) {
        _EnumClass.EAST -> _ClassLevel2()
        _EnumClass.NORTH -> _ClassLevel3()
        _EnumClass.SOUTH -> _ClassLevel4()
        _EnumClass.WEST -> _ClassLevel5()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel5>() }
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: _EnumClass?): String {
    val whenValue = when (value_1) {
        _EnumClass.EAST -> _ClassLevel2()
        _EnumClass.NORTH -> _ClassLevel3()
        _EnumClass.SOUTH -> _ClassLevel4()
        _EnumClass.WEST -> _ClassLevel5()
        null -> _ClassLevel6()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel5>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel6>() }
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel6>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: _EnumClass): String {
    val whenValue = when (value_1) {
        _EnumClass.EAST -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _EnumClass.NORTH -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _EnumClass.SOUTH -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        _EnumClass.WEST -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: _EnumClass?): String {
    val whenValue = when (value_1) {
        _EnumClass.EAST -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _EnumClass.NORTH -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _EnumClass.SOUTH -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        _EnumClass.WEST -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> <!IMPLICIT_CAST_TO_ANY!>false<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}
