// !CHECK_TYPE
// !WITH_SEALED_CLASSES
// !WITH_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 9 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via sealed class).
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _SealedClass): String {
    val whenValue = when (value_1) {
        is _SealedChild1 -> _ClassLevel2()
        is _SealedChild2 -> _ClassLevel3()
        is _SealedChild3 -> _ClassLevel4()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: _SealedClass?): String {
    val whenValue = when (value_1) {
        is _SealedChild1 -> _ClassLevel2()
        is _SealedChild2 -> _ClassLevel3()
        is _SealedChild3 -> _ClassLevel4()
        null -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: _SealedClass): String {
    val whenValue = when (value_1) {
        is _SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is _SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is _SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: _SealedClass?): String {
    val whenValue = when (value_1) {
        is _SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is _SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is _SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: _SealedClassWithObjects): String {
    val whenValue = when (value_1) {
        _SealedWithObjectsChild1 -> _ClassLevel2()
        _SealedWithObjectsChild2 -> _ClassLevel3()
        _SealedWithObjectsChild3 -> _ClassLevel4()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: _SealedClassWithObjects?): String {
    val whenValue = when (value_1) {
        _SealedWithObjectsChild1 -> _ClassLevel2()
        _SealedWithObjectsChild2 -> _ClassLevel3()
        _SealedWithObjectsChild3 -> _ClassLevel4()
        null -> _ClassLevel5()
    }

    whenValue checkType { _<_ClassLevel2>() }
    checkSubtype<_ClassLevel1>(whenValue)

    return ""
}

// TESTCASE NUMBER: 7
fun case_7(value_1: _SealedClassWithObjects): String {
    val whenValue = when (value_1) {
        _SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// TESTCASE NUMBER: 8
fun case_8(value_1: _SealedClassWithObjects?): String {
    val whenValue = when (value_1) {
        _SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        _SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        _SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// TESTCASE NUMBER: 9
fun case_9(value_1: _SealedClassWithObjects): String {
    val whenValue = when (value_1) {
        <!USELESS_IS_CHECK!>is _SealedClassWithObjects<!> -> 10
    }

    whenValue checkType { _<Int>() }
    checkSubtype<Int>(whenValue)

    return ""
}
