open class Foo<T : CharSequence>

fun bar(): Foo<String>? {
    return null
}

fun resolve<caret>Me() {
    val x = bar()
}
