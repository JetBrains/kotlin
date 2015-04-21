// !CHECK_TYPE

package kotlin1

import java.util.*

fun main(args : Array<String>) {
    val al : ArrayList<Int> = ArrayList<Int>()
    checkSubtype<Any>(al.clone()) // A type mismatch on this line means that jdk-annotations were not loaded
}
