import kotlin.collections.*

const val doubleListSize = <!EVALUATED: `3`!>listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9")
).size<!>

const val doubleListSizeOfList = <!EVALUATED: `3`!>listOf(
    listOf("1"),
    listOf("4", "5"),
    listOf("7", "8", "9")
)[2].size<!>

const val doubleListGetSingleElement = <!EVALUATED: `9`!>listOf(
    listOf("1"),
    listOf("4", "5"),
    listOf("7", "8", "9")
)[2][2]<!>

const val doubleListElements = <!EVALUATED: `1; 4, 5; 7, 8, 9`!>listOf(
    listOf("1"),
    listOf("4", "5"),
    listOf("7", "8", "9")
).joinToString(separator = "; ") { it.joinToString() }<!>
