// !CHECK_TYPE
// !WITH_ENUM_CLASSES
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 2
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via enum).
 */

// CASE DESCRIPTION: Checking all types except the correct one in 'when'.
fun case_1(value: _EnumClass): String {
    val whenValue = when (value) {
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

// CASE DESCRIPTION: Checking all types except the correct one in 'when' with null-check branch.
fun case_2(value: _EnumClass?): String {
    val whenValue = when (value) {
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

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when'.
fun case_3(value: _EnumClass): String {
    val whenValue = when (value) {
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

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: _EnumClass?): String {
    val whenValue = when (value) {
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
