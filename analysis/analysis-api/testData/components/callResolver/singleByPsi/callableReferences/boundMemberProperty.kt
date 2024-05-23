class Foo {
    val foo: String
        get() = ""
}

fun test(obj: Foo) {
    consume(<expr>obj::foo</expr>)
}

fun consume(f: () -> String) {}