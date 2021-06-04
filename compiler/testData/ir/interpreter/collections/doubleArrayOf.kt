import kotlin.collections.*

const val doubleArray = arrayOf(
    arrayOf(1, 2, 3),
    arrayOf(4, 5),
    arrayOf(6)
).<!EVALUATED: `1, 2, 3; 4, 5; 6`!>joinToString(separator = "; ") { it.joinToString() }<!>

const val doubleUintArray = arrayOf(
    arrayOf(1u, 2u, 3u),
    arrayOf(4u, 5u),
    arrayOf(6u)
).<!EVALUATED: `1, 2, 3; 4, 5; 6`!>joinToString(separator = "; ") { it.joinToString() }<!>
