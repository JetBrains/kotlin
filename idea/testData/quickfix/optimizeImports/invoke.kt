// "Optimize imports" "false"
// WITH_RUNTIME
// ACTION: Introduce import alias
package ppp

import ppp.invoke<caret>

object Bar
object Foo {
    val bar = Bar
}

fun Foo.bar() = 1
operator fun Bar.invoke() = 2

fun main() {
    println(Foo.bar())
}