// !CHECK_TYPE

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 5
 SENTENCE 1: The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 1
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via else branch).
 */

open class A {}
open class B: A() {}
open class C: B() {}
open class D: C() {}
class E: D() {}

// CASE DESCRIPTION: Checking all types except the correct one (custom types) in 'when' without bound value.
fun case_1(value: Int): String {
    val whenValue = when {
        value == 0 -> B()
        value > 0 && value <= 10 -> C()
        value > 10 && value <= 100 -> D()
        else -> E()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><A>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><C>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><D>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><E>() }
    checkSubtype<C>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<D>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<E>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the correct one (custom types) in 'when' with bound value.
fun case_2(value: Int): String {
    val whenValue = when (value) {
        0 -> B()
        1 -> C()
        2 -> D()
        else -> E()
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><A>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><C>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><D>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><E>() }
    checkSubtype<C>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<D>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<E>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the correct one (numbers) in 'when' without bound value.
fun case_3(value: Int): String {
    val whenValue = when {
        value == 0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        value == 1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        value == 2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        value == 3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        value == 4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toByte()<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Long>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Double>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Float>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Long>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Double>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Float>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Byte>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the correct one (numbers) in 'when' with bound value.
fun case_4(value: Int): String {
    val whenValue = when (value) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>1 + 1L<!>
        2 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.122<!>
        3 -> <!IMPLICIT_CAST_TO_ANY!>1 + -.000f<!>
        4 -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toShort()<!>
        else -> <!IMPLICIT_CAST_TO_ANY!>1 + 10.toByte()<!>
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Long>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Double>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Float>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Long>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Double>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Float>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<Byte>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when' without bound value.
fun case_5(value: Int): String {
    val whenValue = when {
        value == 0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        value > 0 && value <= 10 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        value > 10 && value <= 100 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}

// CASE DESCRIPTION: Checking all types except the Any (implicit cast to any) in 'when' with bound value.
fun case_6(value: Int): String {
    val whenValue = when (value) {
        0 -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        1 -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        2 -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        else -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><String>() }
    whenValue checkType { <!TYPE_MISMATCH!>_<!><() -> Unit>() }
    checkSubtype<Int>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<String>(<!TYPE_MISMATCH!>whenValue<!>)
    checkSubtype<() -> Unit>(<!TYPE_MISMATCH!>whenValue<!>)

    return ""
}
