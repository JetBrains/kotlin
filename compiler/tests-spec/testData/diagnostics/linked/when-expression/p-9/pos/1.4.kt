// !CHECK_TYPE
// !WITH_SEALED_CLASSES
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 9
 SENTENCE: [1] The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 4
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via sealed class).
 */

// CASE DESCRIPTION: Checking correct type in 'when'.
fun case_1(value: _SealedClass): String {
    val whenValue = when (value) {
        is _SealedChild1 -> _ClassLevel2()
        is _SealedChild2 -> _ClassLevel3()
        is _SealedChild3 -> _ClassLevel4()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' with null-check branch.
fun case_2(value: _SealedClass?): String {
    val whenValue = when (value) {
        is _SealedChild1 -> _ClassLevel2()
        is _SealedChild2 -> _ClassLevel3()
        is _SealedChild3 -> _ClassLevel4()
        null -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when'.
fun case_3(value: _SealedClass): String {
    val whenValue = when (value) {
        is _SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is _SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is _SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: _SealedClass?): String {
    val whenValue = when (value) {
        is _SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is _SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is _SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' (equality with objects).
fun case_5(value: _SealedClassWithObjects): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> _ClassLevel2()
        _SealedWithObjectsChild2 -> _ClassLevel3()
        _SealedWithObjectsChild3 -> _ClassLevel4()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' (equality with objects) with null-check branch.
fun case_6(value: _SealedClassWithObjects?): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> _ClassLevel2()
        _SealedWithObjectsChild2 -> _ClassLevel3()
        _SealedWithObjectsChild3 -> _ClassLevel4()
        null -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' (equality with objects).
fun case_7(value: _SealedClassWithObjects): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with null-check branch (equality with objects).
fun case_8(value: _SealedClassWithObjects?): String {
    val whenValue = when (value) {
        _SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct basic type (Int) in 'when' with.
fun case_9(value: _SealedClassWithObjects): String {
    val whenValue = when (value) {
        <!USELESS_IS_CHECK!>is _SealedClassWithObjects<!> -> 10
    }

    whenValue checkType { _<Int>() }
    checkSubtype<Int>(whenValue)

    return ""
}