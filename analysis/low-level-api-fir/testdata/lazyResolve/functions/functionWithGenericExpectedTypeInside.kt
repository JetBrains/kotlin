open class Foo<T : CharSequence>

class Bar : Foo<String>

fun bar() = Bar()

fun resolve<caret>Me() {
    val x: Foo<String> = bar()
}
