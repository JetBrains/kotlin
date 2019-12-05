// FIX: Merge call chain to 'associateBy'
// WITH_RUNTIME
fun getKey(i: Int): Long = 1L

fun test(list: List<Int>) {
    val map: Map<Long, Int> = list.<caret>map { getKey(it) to it }.toMap()
}