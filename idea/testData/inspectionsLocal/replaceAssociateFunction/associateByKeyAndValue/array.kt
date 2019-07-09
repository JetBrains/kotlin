// HIGHLIGHT: INFORMATION
// PROBLEM: Replace 'associate' with 'associateBy'
// FIX: Replace with 'associateBy'
// WITH_RUNTIME
fun getKey(i: Int): Long = 1L
fun getValue(i: Int): String = ""

fun test() {
    arrayOf(1).<caret>associate { getKey(it) to getValue(it) }
}