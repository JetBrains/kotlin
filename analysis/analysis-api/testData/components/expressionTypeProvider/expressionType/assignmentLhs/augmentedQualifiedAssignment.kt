class Foo {
    var value: String = "foo"

    fun test() {
        <expr>this.value</expr> += "bar"
    }
}
