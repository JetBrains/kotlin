class Foo {
    default object {
        val bar: String

        {
            bar = "OK"
        }
    }
}

fun box(): String {
    return Foo.bar
}