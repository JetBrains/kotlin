fun test(foo: Any) {
    if (foo is Foo) {
        if (foo.a is String) {
            <expr>Unit</expr>
        }
    }
}

class Foo(val a: Any)