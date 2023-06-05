// FIR_IDENTICAL
// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^KT-57755

class Foo<T>(var x: T)

fun <K> foo(x: MutableList<K>): Foo<K> = TODO()

fun main() {
    val x = buildList {
        add("")
        val foo = foo(this)
        foo.x = ""
    }
}
