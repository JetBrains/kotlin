namespace `in`.the.loop

import std.io.*

val END = Integer.MAX_VALUE
val START = END - 100

fun main(args : Array<String>) {
    var count = 0
    // For-loop over integrals always terminates
    for (i in START..END)
      count++
    println(count)
}