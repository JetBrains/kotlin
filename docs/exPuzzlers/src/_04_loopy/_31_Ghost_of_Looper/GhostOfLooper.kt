namespace ghost.of.looper

import kotlin.io.*

fun main(args : Array<String>) {
    var i : Short = -1.toShort()
    while (i != 1.toShort())
    // Lots of magic made explicit:
        i = (i.toInt() ushr 1).toShort()
}