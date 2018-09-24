// !CHECK_TYPE
// !WITH_SEALED_CLASSES
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 4
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via sealed class).
 */

// CASE DESCRIPTION: Checking all types except the correct one in 'when'.
fun case_1(value: _SealedClass): String {
    val whenValue = when (value) {
        is _SealedChild1 -> _ClassLevel2()
        is _SealedChild2 -> _ClassLevel3()
        is _SealedChild3 -> _ClassLevel4()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the correct one in 'when' with null-check branch.
fun case_2(value: _SealedClass?): String {
    val whenValue = when (value) {
        is _SealedChild1 -> _ClassLevel2()
        is _SealedChild2 -> _ClassLevel3()
        is _SealedChild3 -> _ClassLevel4()
        null -> _ClassLevel5()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel5>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    checkSubtype<_ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when'.
fun case_3(value: _SealedClass): String {
    val whenValue = when (value) {
        is _SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is _SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is _SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: _SealedClass?): String {
    val whenValue = when (value) {
        is _SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is _SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is _SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking objects except the correct one in 'when'.
fun case_5(value: _SealedClassWithObjects): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> _ClassLevel2()
        _SealedWithObjectsChild2 -> _ClassLevel3()
        _SealedWithObjectsChild3 -> _ClassLevel4()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking objects except the correct one in 'when' with null-check branch.
fun case_6(value: _SealedClassWithObjects?): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> _ClassLevel2()
        _SealedWithObjectsChild2 -> _ClassLevel3()
        _SealedWithObjectsChild3 -> _ClassLevel4()
        null -> _ClassLevel5()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel5>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    checkSubtype<_ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking objects except the Any (implicit cast to any) in 'when'.
fun case_7(value: _SealedClassWithObjects): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking objects except the Any (implicit cast to any) in 'when' with null-check branch.
fun case_8(value: _SealedClassWithObjects?): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the correct one in 'when' with 'else' branch.
fun case_9(value: _SealedClassWithObjects?): String {
    val whenValue = when (value) {
        is _SealedClassWithObjects -> _ClassLevel2()
        else -> _ClassLevel3()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel5>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><_ClassLevel1>() }
    checkSubtype<_ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<_ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}