class Foo<T> {
    companion object {
        fun foo() {}
    }
}

fun usage() {
    <expr>Foo</expr><String>.foo()
}
