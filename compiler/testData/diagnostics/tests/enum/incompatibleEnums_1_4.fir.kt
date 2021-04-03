// !LANGUAGE: +ProhibitComparisonOfIncompatibleEnums

enum class E1 {
    A, B
}

enum class E2 {
    A, B
}

fun foo1(e1: E1, e2: E2) {
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == e2<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 != e2<!>

    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == E2.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>E1.B == e2<!>

    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>E1.A == E2.B<!>

    e1 == E1.A
    E1.A == e1
    e2 == E2.B
    E2.B == e2
}

fun foo2(e1: E1, e2: E2) {
    when (e1) {
        E1.A -> {}
            <!INCOMPATIBLE_TYPES!>E2.A<!> -> {}
            <!INCOMPATIBLE_TYPES!>E2.B<!> -> {}
        e1 -> {}
            <!INCOMPATIBLE_TYPES!>e2<!> -> {}
        else -> {}
    }
}

fun foo3(e1: Enum<E1>, e2: Enum<E2>, e: Enum<*>) {
    e1 == e
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == e2<!>

    e1 == E1.A
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == E2.A<!>

    when (e1) {
        e1 -> {}
        <!INCOMPATIBLE_TYPES!>e2<!> -> {}
        e -> {}
        E1.A -> {}
            <!INCOMPATIBLE_TYPES!>E2.A<!> -> {}
        else -> {}
    }

    when (e) {
        e -> {}
        e2 -> {}
        E1.A -> {}
        E2.A -> {}
        else -> {}
    }
}

interface MyInterface
open class MyOpenClass

fun foo4(e1: E1, i: MyInterface, c: MyOpenClass) {
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == i<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>i == e1<!>

    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == c<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>c == e1<!>

    when (e1) {
            <!INCOMPATIBLE_TYPES!>i<!> -> {}
            <!INCOMPATIBLE_TYPES!>c<!> -> {}
        else -> {}
    }
}

enum class E3 : MyInterface { X, Y }

fun foo5(i: MyInterface, a: Any) {
    E3.X == E3.Y
    E3.X == i
    E3.X == a
}

fun foo6(e1: E1?, e2: E2) {
    E1.A == null
    null == E1.A
    e1 == null
    null == e1

    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == E2.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>E2.A == e1<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e1 == e2<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e2 == e1<!>

    e2 == null
    null == e2
    E1.A == null
    null == E1.A
}

fun foo7(e1: E1?, e2: E2?) {
    e1 == e2 // There should be an IDE-inspection for such cases
}

fun <T> foo8(e1: E1?, e2: E2, t: T) {
    e1 == t
    t == e1

    e2 == t
    t == e2

    E1.A == t
    t == E1.A
}

fun <T, K> foo9(e1: E1?, e2: E2, t: T, k: K) where T : MyInterface, T : MyOpenClass, K : MyInterface {
    <!EQUALITY_NOT_APPLICABLE_WARNING!>e1 == t<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>t == e1<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>e2 == t<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>t == e2<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>E1.A == t<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>t == E1.A<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>E3.X == t<!>

    E3.X == k
    k == E3.X
}

interface Inv<T>

enum class E4 : Inv<Int> { A }

fun foo10(e4: E4, invString: Inv<String>) {
    <!EQUALITY_NOT_APPLICABLE_WARNING!>e4 == invString<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>invString == e4<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>E4.A == invString<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>invString == E4.A<!>
}
