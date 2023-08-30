interface Foo {}

fun Any.test() {
    if (this is Foo) {
        <expr>consume(this)</expr>
    }
}

fun consume(obj: Foo) {}