namespace curse.of.loooper   // BUG!!!

import std.io.*

class Curse<T : Comparable<T>>() {
    fun curse(i : T, j : T) {
        while (i <= j && j <= i && i != j) {
        }
    }
}

fun main(args : Array<String>) {
// BUG:
//    val i : Integer = (128 : Int?) as Integer
//    val j : Integer = (128 : Int?) as Integer
//    while (i <= j && j <= i && i != j) {
//    }
    Curse<Int>.curse(128, 128)
}