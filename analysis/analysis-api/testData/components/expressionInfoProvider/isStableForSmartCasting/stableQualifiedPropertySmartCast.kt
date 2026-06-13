fun test(foo: Any) {
    if (foo is Foo && foo.value is String) {
        println(<expr>foo.value</expr>.length)
    }
}

class Foo(val value: Any)
