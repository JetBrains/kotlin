// "Replace with 'Factory()'" "true"
// WITH_RUNTIME
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class Foo<T> @Deprecated("", ReplaceWith("Factory<T>()")) constructor()
fun <T> Factory(): Foo<T> = TODO()

fun baz() {
    val foo: Foo<Int> = <caret>Foo()
}