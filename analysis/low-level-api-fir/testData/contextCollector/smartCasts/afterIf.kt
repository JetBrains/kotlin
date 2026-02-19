interface Foo {}

fun test(obj: Any) {
    if (obj is Foo) {
        consume(obj)
    }
    <expr>obj.hashCode()</expr>
}

fun consume(obj: Foo) {}