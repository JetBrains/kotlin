// FIX: Merge call chain to 'associate'
// WITH_RUNTIME
fun getKey(i: Int): Long = 1L
fun getValue(i: Int): String = ""

fun test(list: List<Int>) {
    val map: Map<Long, String> = list.<caret>map { getKey(it) to getValue(it) }.toMap()
}