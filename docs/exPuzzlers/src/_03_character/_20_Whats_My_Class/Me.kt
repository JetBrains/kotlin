namespace whats.my.`class`

import std.io.*
import std.string.*
import std.jutils.*

class Me() {
    fun main() {
        println(getJavaClass().getCanonicalName()?.replaceAllSubstrings(".", "/"))

        // Regex is denoted explicitly
        println(getJavaClass().getCanonicalName()?.replaceAllWithRegex(".", "/"))
    }
}

fun main(args : Array<String>) {
    Me().main()
}
