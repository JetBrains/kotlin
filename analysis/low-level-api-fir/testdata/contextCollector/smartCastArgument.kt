interface Foo {}

fun test(obj: Any) {
    if (obj is Foo) {
        consume(<expr>obj</expr>)
    }
}

fun consume(obj: Any) {}