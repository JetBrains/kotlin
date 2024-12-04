interface Foo {}

fun test(obj: Any, another: Foo) {
    if (obj is Foo) {
        <expr>consume(another)</expr>
    }
}

fun consume(obj: Foo) {}