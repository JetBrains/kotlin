class Foo {
    companion object {
        val bar: String

        {
            bar = "OK"
        }
    }
}

fun box(): String {
    return Foo.bar
}