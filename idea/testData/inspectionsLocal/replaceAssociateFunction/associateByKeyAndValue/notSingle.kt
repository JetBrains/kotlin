// PROBLEM: none
// WITH_RUNTIME
fun getKey(i: Int): Long = 1L
fun getValue(i: Int): String = ""

fun test() {
    listOf(1).<caret>associate {
        val key = getKey(it)
        val value = getValue(it)
        key to value
    }
}