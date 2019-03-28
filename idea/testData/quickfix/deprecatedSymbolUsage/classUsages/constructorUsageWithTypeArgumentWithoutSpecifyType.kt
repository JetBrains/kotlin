// "Replace with 'Factory()'" "true"
// WITH_RUNTIME

class Foo<T> @Deprecated("", ReplaceWith("Factory<T>()")) constructor()
fun <T> Factory(): Foo<T> = TODO()

fun baz() {
    val foo: Foo<Int> = <caret>Foo()
}