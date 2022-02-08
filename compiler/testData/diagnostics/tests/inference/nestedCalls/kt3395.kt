// FIR_IDENTICAL
//KT-3395 mapOf function can't be used as literal
package b

import java.util.ArrayList

public fun <T> query(t: T, args: Map<String, Any>): List<T> {
    return ArrayList<T>()
}

fun test(pair: Pair<String, Int>) {
    val id = "Hello" // variable is marked as unused

    println("Some" + query(0, mapOf(id to 1)))

    println("Some" + query(0, mapOf(pair)))
}


//from standard library
fun <K, V> mapOf(vararg values: Pair<K, V>): Map<K, V> { throw Exception() }

infix fun <A,B> A.to(that: B): Pair<A, B> { throw Exception() }

fun println(message : Any?) { throw Exception() }

class Pair<out A, out B> () {}

//short example
fun <T> foo(t: T) = t

fun test(t: String) {

    println("Some" + foo(t)) // t was marked with black square
}
