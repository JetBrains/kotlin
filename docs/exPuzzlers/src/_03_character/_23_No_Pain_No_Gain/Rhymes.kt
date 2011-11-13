namespace no.pain.no.gain

import std.io.*

val rnd = java.util.Random();

fun main(args : Array<String>) {
    // The erroneous code does not compile:
//    var word : StringBuffer? = null
//    when (rnd.nextInt(2)) {
//        1 => word = StringBuffer('P')
//        2 => word = StringBuffer('G')
//        else => word = StringBuffer('M')
//    }
//    word?.append('a')
//    word?.append('i')
//    word?.append('n')
//    println(word)

// A natural implementation can't possibly have two problems out of three:
//  * fall-through cases
//  * StringBuffer(int) called with 'P'
    val word =
        when (rnd.nextInt(2)) {
            1 => StringBuffer().append('P')
            2 => StringBuffer().append('G')
            else => StringBuffer().append('M')
        }
    word?.append('a')
    word?.append('i')
    word?.append('n')
    println(word)
}