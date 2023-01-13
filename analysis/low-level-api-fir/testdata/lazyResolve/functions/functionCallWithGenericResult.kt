open class Foo<T : CharSequence>

fun bar(): Foo<String>? {
    return null
}

fun resolveMe() {
    val x = bar()
}
