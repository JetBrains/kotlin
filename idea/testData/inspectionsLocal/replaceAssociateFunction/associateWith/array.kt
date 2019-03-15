// PROBLEM: none
// WITH_RUNTIME
fun getValue(i: Int): String = ""

fun test() {
    arrayOf(1).<caret>associate { it to getValue(it) }
}