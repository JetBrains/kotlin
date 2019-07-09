// PROBLEM: Replace 'associate' with 'associateBy'
// FIX: Replace with 'associateBy'
// WITH_RUNTIME
fun getKey(i: Int): Long = 1L

fun test() {
    arrayOf(1).<caret>associate { getKey(it) to it }
}