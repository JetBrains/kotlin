interface Foo

val Foo.foo: String
    get() = ""

fun test() {
    consume(<expr>Foo::foo</expr>)
}

fun consume(f: (Foo) -> String) {}