namespace elementary

import kotlin.io.*

fun main(args : Array<String>) {
    // Problematic case does not compile
//    println(12345 + 5432l)

    // Correct syntax:
    println(12345 + 5432.toLong())
}