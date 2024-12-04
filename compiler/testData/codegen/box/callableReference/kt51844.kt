// WITH_STDLIB

abstract class Foo {
    abstract fun contains(x: Int);
}

// ERROR: Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly
fun Foo.contains(vararg xs: Int) = xs.forEach(this::contains)

fun box(): String {
    object : Foo() {
        override fun contains(x: Int) {}
    }.contains(1)
    return "OK"
}
