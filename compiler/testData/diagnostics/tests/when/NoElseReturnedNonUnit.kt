fun foo(x: Int): Any {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        2 -> 0
    }
}