// CHECK_TYPE

sealed class A {
    class A1 : A()
    class A2 : A()
}

sealed class B {
    class B1 : B()
    class B2 : B()
}

fun foo(a: A) {
    if (<!USELESS_IS_CHECK!>a !is B<!>) return

    <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>a<!>) {
        is A.A1 -> ""
        is A.A2 -> "v"
    }.length

    <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>a<!>) {
        is A.A1 -> ""
        is A.A2 -> "v"
    }.length // OK

    <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>a<!>) {
        is A.A1 -> ""
        is A.A2 -> "v"
        is <!INCOMPATIBLE_TYPES!>B.B1<!> -> "..." // should be warning: unreachable code
    }.length // OK

    <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>a<!>) {
        is A.A1 -> ""
        is <!INCOMPATIBLE_TYPES!>B.B1<!> -> "..."
        is A.A2 -> "v"
    }.length // OK

    <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>a<!>) {
        is A.A1 -> ""
        is <!INCOMPATIBLE_TYPES!>B.B1<!> -> "..."
    }.length
}
