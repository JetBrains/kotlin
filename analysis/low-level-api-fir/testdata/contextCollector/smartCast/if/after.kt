interface Foo {}

fun test(obj: Any) {
    if (obj is Foo) {
        consume(obj)
    }

    <expr>call(obj)</expr>
}

fun consume(obj: Foo) {}
fun call(obj: Any) {}