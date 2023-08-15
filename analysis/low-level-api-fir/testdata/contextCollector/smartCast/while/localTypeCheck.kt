interface Foo {
    val parent: Any
}

fun test(initialValue: Foo) {
    var current: Any = initialValue

    while (current is Foo) {
        <expr>consume(current)</expr>
        current = current.parent
    }
}

fun consume(foo: Foo) {}