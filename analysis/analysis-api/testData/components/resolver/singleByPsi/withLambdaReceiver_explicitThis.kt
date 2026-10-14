class Test {
    var foo = 1

    fun test() {
        with(Test()) {
            <expr>this.foo</expr> = foo
        }
    }
}
