// PROBLEM: Replace 'associateTo' with 'associateByTo'
// FIX: Replace with 'associateByTo'
// WITH_RUNTIME
fun getKey(i: Int): Long = 1L

fun test() {
    val destination = mutableMapOf<Long, Int>()
    arrayOf(1).<caret>associateTo(destination) { getKey(it) to it }
}