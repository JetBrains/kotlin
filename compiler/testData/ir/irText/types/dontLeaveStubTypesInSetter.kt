// FIR_IDENTICAL
// WITH_STDLIB

class Foo<T>(var x: T)

fun <K> foo(x: MutableList<K>): Foo<K> = TODO()

fun runMe() {
    val x = buildList {
        add("")
        val foo = foo(this)
        foo.x = ""
    }
}
