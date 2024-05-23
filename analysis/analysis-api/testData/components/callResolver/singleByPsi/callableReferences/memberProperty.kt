class Foo {
    val foo: String
        get() = ""
}

fun test() {
    consume(<expr>Foo::foo</expr>)
}

fun consume(f: (Foo) -> String) {}