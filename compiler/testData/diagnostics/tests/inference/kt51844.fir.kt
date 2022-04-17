// WITH_STDLIB

abstract class Foo {
}

// ERROR: Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly
fun Foo.contains(vararg xs: Int) = xs.forEach(this::contains)

fun box(): String {
    return "OK"
}
