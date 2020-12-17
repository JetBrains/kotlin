enum class Foo(val text: String) {
    FOO("foo");
    companion object {
        val first = values()[0]
    }
}

fun box(): String {
    Foo.FOO
    return if (Foo.first === Foo.FOO) "OK" else "FAIL: ${Foo.first}"
}