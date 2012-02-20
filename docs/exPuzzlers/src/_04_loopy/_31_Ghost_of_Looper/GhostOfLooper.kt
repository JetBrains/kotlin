namespace ghost.of.looper

import std.io.*

fun main(args : Array<String>) {
    var i : Short = -1.short
    while (i != 1.short)
    // Lots of magic made explicit:
        i = (i.int ushr 1).short
}