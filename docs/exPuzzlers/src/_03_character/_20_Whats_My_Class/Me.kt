namespace whats.my.`class`

import std.io.*
import std.*
import std.string.*
import typeinfo.*

class Me {
    fun main() {
        (this as Object).getClass()?.getCanonicalName()?.replaceAll(".", "/")
    }
}

fun main(args : Array<String>) {
    // Note: \u000A is Unicode representation of linefeed (LF)
    val c : Char = 0x000A .chr;
    println(c);
}
