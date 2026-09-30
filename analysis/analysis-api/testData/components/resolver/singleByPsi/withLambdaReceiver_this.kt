class Test {
    var foo = 1

    fun test() {
        with(Test()) {
            <expr>this</expr>.foo = foo
        }
    }
}
