// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

object Foo {
    fun <T> bar(fn: KFunction1<T, Boolean>): String = ""
    fun <T, U> bar(fn: KFunction2<T, U, Boolean>): Int = 10
}

class A
class B

fun A.test() = true // everything is OK without this line
fun B.test() = true

fun main() {
    val x = Foo.bar(B::test) // ambiguity in NI
}