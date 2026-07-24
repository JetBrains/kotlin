fun test(foo: Any) {
    if (foo is Foo && foo.holder.value is String) {
        println(<expr>foo.holder.value</expr>.length)
    }
}

class Foo(val holder: Holder)

class Holder(val value: Any)
