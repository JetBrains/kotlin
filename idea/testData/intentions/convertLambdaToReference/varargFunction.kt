// IS_APPLICABLE: false
// WITH_RUNTIME
fun test(i: Int?): IntArray? {
    return i?.let { <caret>intArrayOf(it) }
}