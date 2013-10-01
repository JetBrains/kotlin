fun foo(x: Int): Any {
    val v = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        2 -> 0
    }
    return v
}