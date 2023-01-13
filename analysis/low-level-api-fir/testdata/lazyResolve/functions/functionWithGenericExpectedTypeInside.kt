open class Foo<T : CharSequence>

class Bar : Foo<String>

fun bar() = Bar()

fun resolveMe() {
    val x: Foo<String> = bar()
}
