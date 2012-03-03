namespace long.division

import kotlin.io.*

fun main(args : Array<String>) {
    // Problematic case does not compile
//    val MICROS_PER_DAY_ : Long = 24 * 60 * 60 * 1000 * 1000;
//    val MILLIS_PER_DAY_ : Long = 24 * 60 * 60 * 1000

    // Solution:
    val MICROS_PER_DAY : Long = 24.toLong() * 60 * 60 * 1000 * 1000;
    val MILLIS_PER_DAY : Long = 24.toLong() * 60 * 60 * 1000

    println(MICROS_PER_DAY / MILLIS_PER_DAY)
}
