fun foo(a: Int) {
    @ann
    when (a) {
        1 -> {}
    }
}

annotation class ann
