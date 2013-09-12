fun foo(x: Int) {
    r {
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            2 -> 0
        }
    }
}

fun r(f: () -> Int) {
    f()
}