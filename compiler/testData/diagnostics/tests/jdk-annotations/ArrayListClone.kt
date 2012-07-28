package kotlin1

import java.util.*

fun main(args : Array<String>) {
    val al : ArrayList<Int> = ArrayList<Int>()
    al.clone() : Any // A type mismatch on this line means that jdk-annotations were not loaded
}
