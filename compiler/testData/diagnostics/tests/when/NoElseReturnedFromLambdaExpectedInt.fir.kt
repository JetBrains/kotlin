fun foo(x: Int) {
    r {
        when (x) {
            2 -> 0
        }
    }
}

fun r(f: () -> Int) {
    f()
}