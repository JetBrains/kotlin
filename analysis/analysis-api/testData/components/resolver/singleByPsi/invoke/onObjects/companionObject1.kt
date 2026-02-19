class Foo private constructor() {
    companion object {
        operator fun invoke() {}
    }
}

fun test() {
    Fo<caret>o()
}