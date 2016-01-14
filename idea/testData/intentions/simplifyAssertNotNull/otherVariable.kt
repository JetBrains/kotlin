// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo(p: Array<String?>) {
    val v1 = p[0]
    val v2 = p[1]
    <caret>assert(v1 != null)
}