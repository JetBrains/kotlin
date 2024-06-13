val foo: String
    get() = ""

fun test(foo: Foo) {
    consume(<expr>::foo</expr>)
}

fun consume(f: () -> String) {}

class Foo