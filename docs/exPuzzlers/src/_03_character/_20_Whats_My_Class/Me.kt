namespace whats.my.`class`

import kotlin.io.*
import kotlin.string.*
import kotlin.jutils.*

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
