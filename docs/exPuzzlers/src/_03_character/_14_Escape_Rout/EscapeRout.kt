namespace escape.rout

import std.io.*
import std.*

fun main(args : Array<String>) {
    // \u0022 is the Unicode escape for double quote (")
    println("a\u0022.length( ) + \u0022b".length)

    // The actual string:
    println("a\u0022.length( ) + \u0022b")
}