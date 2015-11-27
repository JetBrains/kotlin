fun foo(x: Int) {
    val y: Unit = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        2 -> {}
        3 -> {}
    }
    return y
}