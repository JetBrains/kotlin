package kotlin1

import java.util.*

fun main(args : Array<String>) {
    val al : ArrayList<Int> = ArrayList<Int>()

    // A type mismatch on this line means that jdk-annotations were not loaded
    al.toArray(Array<Int>(3, {1})) : Array<Int>
}
