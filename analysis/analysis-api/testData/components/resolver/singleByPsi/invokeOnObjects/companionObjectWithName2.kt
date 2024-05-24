class Foo private constructor() {
    companion object Helper {
        operator fun invoke() {}
    }
}

fun test() {
    <expr>Foo</expr>()
}