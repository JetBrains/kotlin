interface Foo {
    val parent: Any
}

fun test(initialValue: Foo) {
    var current: Any = initialValue

    while (current is Foo) {
        consume(current)
        current = current.parent
    }

    <expr>call(current)</expr>
}

fun consume(foo: Foo) {}
fun call(obj: Any) {}