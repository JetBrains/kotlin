interface Foo {}

fun test(obj: Any) {
    if (obj is Foo) {
        <expr>consume(obj)</expr>
    }
}

fun consume(obj: Foo) {}