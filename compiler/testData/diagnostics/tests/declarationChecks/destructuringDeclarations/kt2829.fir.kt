package test

fun a(s: String) { // <- ERROR
    val (x, y) = Pair("", s)
    println(x + y)
}

fun b(s: String) {
    val x = Pair("", s)
    println(x)
}

//from library
data class Pair<A, B>(val a: A, val b: B)

fun println(a: Any?) = a