namespace line.printer

import std.io.*
import std.*

fun main(args : Array<String>) {
    // Note: \u000A is Unicode representation of linefeed (LF)
    val c : Char = 0x000A .chr;
    println(c);
}
