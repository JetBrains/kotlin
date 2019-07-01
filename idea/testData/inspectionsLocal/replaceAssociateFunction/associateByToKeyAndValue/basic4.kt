// HIGHLIGHT: INFORMATION
// PROBLEM: Replace 'associateTo' with 'associateByTo'
// FIX: Replace with 'associateByTo'
// WITH_RUNTIME
fun getKey(i: Int): Long = 1L
fun getValue(i: Int): String = ""

fun test() {
    val destination = mutableMapOf<Long, String>()
    listOf(1).<caret>associateTo(destination, { getKey(it) to getValue(it) })
}
