class Outer {
    class Nested
    inner class Inner
}

fun test() {
    Outer()::Inner
    Outer()::Nested
}
