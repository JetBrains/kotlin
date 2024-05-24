package foo

class Foo private constructor() {
    companion object {
        operator fun invoke() {}
    }
}

fun test() {
    foo.<expr>Foo</expr>()
}