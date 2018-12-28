// !CHECK_TYPE
// !WITH_CLASSES
// !WITH_ENUM_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
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

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

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

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

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

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

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

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}
