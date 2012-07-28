// KT-524 sure() returns T

package StringBuilder

import java.lang.StringBuilder

//import kotlin.io.*
//import java.io.*

fun main(args : Array<String>) {
}

val Int.bd : StringBuilder get() = StringBuilder(this.toString())
fun StringBuilder.plus(other : StringBuilder) : StringBuilder = this.append(other).sure1() // !!!

fun <T : Any> T?.sure1() : T { return if (this != null) this else throw NullPointerException() }
