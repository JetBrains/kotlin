// !CHECK_TYPE

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 9 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via sealed class).
 * HELPERS: classes, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: SealedClass): String {
    val whenValue = when (value_1) {
        is SealedChild1 -> ClassLevel2()
        is SealedChild2 -> ClassLevel3()
        is SealedChild3 -> ClassLevel4()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: SealedClass?): String {
    val whenValue = when (value_1) {
        is SealedChild1 -> ClassLevel2()
        is SealedChild2 -> ClassLevel3()
        is SealedChild3 -> ClassLevel4()
        null -> ClassLevel5()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel5>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    checkSubtype<ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: SealedClass): String {
    val whenValue = when (value_1) {
        is SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: SealedClass?): String {
    val whenValue = when (value_1) {
        is SealedChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is SealedChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is SealedChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
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

// TESTCASE NUMBER: 5
fun case_5(value_1: SealedClassWithObjects): String {
    val whenValue = when (value_1) {
        SealedWithObjectsChild1 -> ClassLevel2()
        SealedWithObjectsChild2 -> ClassLevel3()
        SealedWithObjectsChild3 -> ClassLevel4()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: SealedClassWithObjects?): String {
    val whenValue = when (value_1) {
        SealedWithObjectsChild1 -> ClassLevel2()
        SealedWithObjectsChild2 -> ClassLevel3()
        SealedWithObjectsChild3 -> ClassLevel4()
        null -> ClassLevel5()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel5>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    checkSubtype<ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 7
fun case_7(value_1: SealedClassWithObjects): String {
    val whenValue = when (value_1) {
        SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// TESTCASE NUMBER: 8
fun case_8(value_1: SealedClassWithObjects?): String {
    val whenValue = when (value_1) {
        SealedWithObjectsChild1 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        SealedWithObjectsChild2 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        SealedWithObjectsChild3 -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
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

// TESTCASE NUMBER: 9
fun case_9(value_1: SealedClassWithObjects?): String {
    val whenValue = when (value_1) {
        is SealedClassWithObjects -> ClassLevel2()
        else -> ClassLevel3()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel5>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel4>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel3>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><ClassLevel1>() }
    checkSubtype<ClassLevel5>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel4>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<ClassLevel3>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}
