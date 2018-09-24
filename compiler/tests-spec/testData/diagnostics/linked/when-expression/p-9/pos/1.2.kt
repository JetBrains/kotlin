// !CHECK_TYPE
// !WITH_CLASSES
// !WITH_ENUM_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 2
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via enum).
 */

// CASE DESCRIPTION: Checking correct type in 'when'.
fun case_1(value: _EnumClass): String {
    val whenValue = when (value) {
        _EnumClass.EAST -> _ClassLevel2()
        _EnumClass.NORTH -> _ClassLevel3()
        _EnumClass.SOUTH -> _ClassLevel4()
        _EnumClass.WEST -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' with null-check branch.
fun case_2(value: _EnumClass?): String {
    val whenValue = when (value) {
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

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when'.
fun case_3(value: _EnumClass): String {
    val whenValue = when (value) {
        _EnumClass.EAST -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _EnumClass.NORTH -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _EnumClass.SOUTH -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        _EnumClass.WEST -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: _EnumClass?): String {
    val whenValue = when (value) {
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
