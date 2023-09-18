interface Foo {}

fun test(obj: Any) {
    <expr>obj.hashCode()</expr>
    if (obj is Foo) {
        consume(obj)
    }
}

fun consume(obj: Foo) {}