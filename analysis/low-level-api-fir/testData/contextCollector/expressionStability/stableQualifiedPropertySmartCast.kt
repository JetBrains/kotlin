fun test(foo: Foo) {
    if (foo.value is String) {
        <expr>foo.value</expr>
    }
}

class Foo(val value: Any)
