namespace ghost.of.looper

import std.io.*

fun main(args : Array<String>) {
    var i : Short = -1.sht
    while (i != 1.sht)
    // Lots of magic made explicit:
        i = (i.int ushr 1).sht
}