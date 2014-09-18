fun foo() {
    when (e) {
        is T<X> -> a
        in f () -> a
        !is T<X> -> a
        !in f() -> a
        f() -> a
    }
}