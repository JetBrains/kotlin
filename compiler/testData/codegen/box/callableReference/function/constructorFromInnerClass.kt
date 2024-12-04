var result = "failed"

class Foo {
    inner class Bar {
        constructor() {
            result = "OK"
        }
    }
}

fun box(): String {
    val a: Foo.() -> Foo.Bar = Foo::Bar
    Foo().a()
    return result
}