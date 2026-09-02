fun Any.test() {
    if (this is Foo) {
        <expr>boo</expr>
    }
}

interface Foo {
    val boo: Int
}
