namespace dos.equis

import std.io.*

fun main(args : Array<String>) {
    // Problematic case does not compile
    val x = 'X'
    val i = 0
    print(if (true) x else 0)
    print(if (false) 0 else x)

    // Mixed computations do not compile:
//    val int : Int = if (true) x else 0
//    val char : Char = if (false) 0 else x
}