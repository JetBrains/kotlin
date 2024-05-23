class Foo(val value: String)

fun test() {
    consume(<expr>::Foo</expr>)
}

fun consume(f: (String) -> Foo) {}