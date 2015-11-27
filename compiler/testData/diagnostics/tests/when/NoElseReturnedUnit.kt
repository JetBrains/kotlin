fun foo(x: Int) {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        2 -> {}
        3 -> {}
    }
}