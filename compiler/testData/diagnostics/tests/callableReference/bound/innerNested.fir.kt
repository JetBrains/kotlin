class Outer {
    class Nested
    inner class Inner
}

fun test() {
    Outer()::Inner
    <!UNRESOLVED_REFERENCE!>Outer()::Nested<!>
}
