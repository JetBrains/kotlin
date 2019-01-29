// LANGUAGE_VERSION: 1.2
// PROBLEM: none
// WITH_RUNTIME
fun getValue(i: Int): String = ""

fun test() {
    listOf(1).<caret>associate { it to getValue(it) }
}