enum class E1 {
    A, B
}

enum class E2 {
    A, B
}

fun foo1(e1: E1, e2: E2) {
    <!INCOMPATIBLE_ENUM_COMPARISON!>e1 == e2<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>e1 != e2<!>

    <!INCOMPATIBLE_ENUM_COMPARISON!>e1 == E2.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>E1.B == e2<!>

    <!INCOMPATIBLE_ENUM_COMPARISON!>E1.A == E2.B<!>

    e1 == E1.A
    E1.A == e1
    e2 == E2.B
    E2.B == e2
}

fun foo2(e1: E1, e2: E2) {
    when (e1) {
        E1.A -> {}
        <!INCOMPATIBLE_ENUM_COMPARISON!>E2.A<!> -> {}
        <!INCOMPATIBLE_ENUM_COMPARISON!>E2.B<!> -> {}
        e1 -> {}
        <!INCOMPATIBLE_ENUM_COMPARISON!>e2<!> -> {}
        else -> {}
    }
}

fun foo3(e1: Enum<E1>, e2: Enum<E2>, e: Enum<*>) {
    e1 == e
    e1 == e2

    e1 == E1.A
    e1 == E2.A

    when (e1) {
        e1 -> {}
        e2 -> {}
        e -> {}
        E1.A -> {}
        E2.A -> {}
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
