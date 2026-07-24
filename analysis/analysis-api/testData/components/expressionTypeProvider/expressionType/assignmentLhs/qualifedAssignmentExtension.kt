class Foo {
    var value: String = "foo"
}

fun Foo.test() {
    <expr>this.value</expr> = "bar"
}
