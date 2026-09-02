interface Foo {
    fun test() {
        if (this is Impl) {
            <expr>boo</expr>
        }
    }
}

interface Impl : Foo {
    val boo: Int
}
