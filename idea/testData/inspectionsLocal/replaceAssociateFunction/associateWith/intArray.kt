// PROBLEM: none
// WITH_RUNTIME
fun getValue(i: Int): String = ""

fun test() {
    intArrayOf(1).<caret>associate { it to getValue(it) }
}