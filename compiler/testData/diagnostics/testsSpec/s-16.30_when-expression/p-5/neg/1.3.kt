// !CHECK_TYPE

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 5
 SENTENCE 1: The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 3
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via boolean bound value).
 */

open class A {}
open class B: A() {}
open class C: B() {}
class D: C() {}

// CASE DESCRIPTION: Checking all types except the correct one in 'when'.
fun case_1(value: Boolean): String {
    val whenValue = when (value) {
        true -> B()
        false -> C()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><D>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><C>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><A>() }
    checkSubtype<D>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<C>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the correct one in 'when' with null-check branch.
fun case_2(value: Boolean?): String {
    val whenValue = when (value) {
        true -> B()
        false -> C()
        null -> D()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><D>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><C>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><A>() }
    checkSubtype<D>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<C>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when'.
fun case_3(value: Boolean): String {
    val whenValue = when (value) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> <!IMPLICIT_CAST_TO_ANY!>""<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: Boolean?): String {
    val whenValue = when (value) {
        true -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        false -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        null -> <!IMPLICIT_CAST_TO_ANY!>""<!>
    }


    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}


