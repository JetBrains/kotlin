object Bar {
    operator fun invoke(x: String) {}
}

fun foo() {
    Bar("asd")
}
