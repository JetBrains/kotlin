// TARGET_BACKEND: JVM
// FILE: A.kt

package test

import java.util.*

fun printStream() = System.out
fun list() = Collections.emptyList<String>()
fun array(a: Array<Int>) = Arrays.copyOf(a, 2)

// FILE: B.kt

import java.io.PrintStream
import java.util.ArrayList
import test.*

// To check that flexible types are loaded
class Inv<T>
fun <T> inv(t: T): Inv<T> = Inv<T>()

fun box(): String {
    printStream().checkError()
    val p: Inv<PrintStream> = inv(printStream())
    val p1: Inv<PrintStream?> = inv(printStream())

    list().size
    val l: Inv<List<String>> = inv(list())
    val l1: Inv<MutableList<String>?> = inv(list())

    val a = array(arrayOfNulls<Int>(1) as Array<Int>)
    a[0] = 1
    val a1: Inv<Array<Int>> = inv(a)
    val a2: Inv<Array<out Int>?> = inv(a)

    return "OK"
}
