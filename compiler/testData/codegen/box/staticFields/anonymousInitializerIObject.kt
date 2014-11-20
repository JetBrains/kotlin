object Foo {
    val bar: String

    {
        bar = "OK"
    }
}

fun box(): String {
    return Foo.bar
}