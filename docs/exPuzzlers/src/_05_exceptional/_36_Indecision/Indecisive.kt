namespace indecision

import std.io.*

fun main(args : Array<String>) {
    println(decision())
}

fun decision() : Boolean {
    try {
// Problematic code is illegal, as Java Puzzlers recommend:
//        return true;
    } finally {
        return false;
    }
}
