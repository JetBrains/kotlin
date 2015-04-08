object Foo {
    val bar: String

    init {
        bar = "OK"
    }
}

fun box(): String {
    return Foo.bar
}