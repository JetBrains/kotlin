// IS_APPLICABLE: false
fun foo(p: List<String?>): Int {
    val v = p[0]
    <caret>if (v != null) return -1
    return 0
}