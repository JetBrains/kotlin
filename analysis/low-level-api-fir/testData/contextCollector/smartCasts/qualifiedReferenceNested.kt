fun test(foo: Any) {
    if (foo is Foo) {
        if (foo.a is Bar) {
            if (foo.a.b is String) {
                <expr>Unit</expr>
            }
        }
    }
}

class Foo(val a: Any)
class Bar(val b: Any)