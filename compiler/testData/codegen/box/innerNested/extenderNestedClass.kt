object Foo {
    open class Bar(val bar: String)
}

class Baz: Foo.Bar("OK")

fun box(): String {
    return Baz().bar
}
