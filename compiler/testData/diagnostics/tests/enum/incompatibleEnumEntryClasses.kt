// FIR_IDENTICAL
// !LANGUAGE: -ProhibitComparisonOfIncompatibleEnums

interface I {
    fun foo()
}

enum class E1 : I {
    A {
        override fun foo() {
            <!EQUALITY_NOT_APPLICABLE!>this == E2.A<!>

            val q = this
            when (q) {
                this -> {}
                E1.A -> {}
                E1.B -> {}
                <!INCOMPATIBLE_TYPES!>E2.A<!> -> {}
                <!INCOMPATIBLE_TYPES!>E2.B<!> -> {}
                else -> {}
            }
        }
    },
    B {
        override fun foo() {

        }
    }
}

enum class E2 : I {
    A {
        override fun foo() {

        }
    },
    B {
        override fun foo() {

        }
    }
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
