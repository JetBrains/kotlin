// PROBLEM: none

import kotlin.reflect.KClass

open class Foo {
    init {
        test(<caret>this::class)
    }
}

private fun test(c: KClass<out Foo>) {
//     println(c)
}

class Bar : Foo()

fun main(args: Array<String>) {
    Foo()
    Bar()
}