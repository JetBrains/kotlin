// !CHECK_TYPE

sealed class A {
    class A1 : A()
    class A2 : A()
}

sealed class B {
    class B1 : B()
    class B2 : B()
}

fun foo(a: A) {
    if (a !is B) return

    when (a) {
        is A.A1 -> ""
        is A.A2 -> "v"
    }.length

    when (a) {
        is A.A1 -> ""
        is A.A2 -> "v"
    }.length // OK

    when (a) {
        is A.A1 -> ""
        is A.A2 -> "v"
        is B.B1 -> "..." // should be warning: unreachable code
    }.length // OK

    when (a) {
        is A.A1 -> ""
        is B.B1 -> "..."
        is A.A2 -> "v"
    }.length // OK

    <!NO_ELSE_IN_WHEN!>when<!> (a) {
        is A.A1 -> ""
        is B.B1 -> "..."
    }.length
}
