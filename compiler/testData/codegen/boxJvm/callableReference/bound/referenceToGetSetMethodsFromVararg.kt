// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

fun <T> bar0(vararg a: T) = test(a::get)
fun <T> bar1(vararg a: T) = test(a::set)

fun <T> bar2(a: Array<out T>) = test(a::get)
fun <T> bar3(a: Array<out T>) = test(a::set)

fun <T> bar4(a: Array<in T>) = test(a::get)
fun <T> bar5(a: Array<in T>) = test(a::set)

fun <F> test(f: F): String = f.toString()

fun box(): String {
    val getMethod = "fun kotlin.Array<T>.get(kotlin.Int): T"
    val setMethod = "fun kotlin.Array<T>.set(kotlin.Int, T): kotlin.Unit"

    val b0 = bar0("")
    val b1 = bar1("")

    assertEquals(getMethod, b0)
    assertEquals(setMethod, b1)

    val b2 = bar2(arrayOf(""))
    val b3 = bar3(arrayOf(""))

    assertEquals(getMethod, b2)
    assertEquals(setMethod, b3)

    val b4 = bar4(arrayOf(""))
    val b5 = bar5(arrayOf(""))

    assertEquals(getMethod, b4)
    assertEquals(setMethod, b5)

    return "OK"
}
