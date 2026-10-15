class Test {
    var foo = 1

    fun test() {
        with(Test()) {
            this.foo = <expr>foo</expr>
        }
    }
}
