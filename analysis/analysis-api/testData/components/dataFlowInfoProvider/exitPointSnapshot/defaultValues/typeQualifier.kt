fun test() {
    <expr>Foo</expr>.Bar().bar()
}

class Foo {
    class Bar {
        fun bar() {}
    }
}