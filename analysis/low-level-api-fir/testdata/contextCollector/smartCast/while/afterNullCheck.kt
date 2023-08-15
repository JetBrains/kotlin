interface Foo {
    val isActive: Boolean
    val parent: Any
}

fun test(foo: Foo?) {
    while (foo!!.isActive) {
        consume(foo.parent as Foo)
        break
    }

    <expr>consume(foo)</expr>
}

fun consume(foo: Foo) {}