// PROBLEM: Replace 'associateTo' with 'associateWithTo'
// FIX: Replace with 'associateWithTo'
// WITH_RUNTIME
fun getValue(i: Int): String = ""

fun associateWithTo() {
    val destination = mutableMapOf<Int, String>()
    listOf(1).<caret>associateTo(destination) { Pair(it, getValue(it)) }
}
