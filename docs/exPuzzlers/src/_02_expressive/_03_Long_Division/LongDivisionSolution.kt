namespace long.division.solution

import std.io.*

fun main(args : Array<String>) {
    val MICROS_PER_DAY : Long = 24.toLong() * 60 * 60 * 1000 * 1000;
    val MILLIS_PER_DAY : Long = 24.toLong() * 60 * 60 * 1000

    println(MICROS_PER_DAY / MILLIS_PER_DAY)
}
