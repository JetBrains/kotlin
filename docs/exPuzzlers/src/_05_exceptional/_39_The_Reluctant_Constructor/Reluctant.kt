namespace the.reluctant.constructor

import std.io.*

public class Reluctant() {
    val internalInstance = Reluctant() // Recursion is obvious
    {
        throw Exception("I'm not coming out")
    }
}

fun main(args : Array<String>) {
    try {
        val b = Reluctant()
        println("Surprise!")
    }
    catch (ex : Exception) {
        println("I told you so")
    }
}