// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class Foo<T>(var x: T)

fun <K> foo(x: MutableList<K>): Foo<K> = TODO()

fun main() {
    val x = buildList {
        add("")
        val foo = foo(this)
        foo.x = ""
    }
}
