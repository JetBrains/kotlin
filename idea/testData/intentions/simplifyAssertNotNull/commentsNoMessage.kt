// INTENTION_TEXT: "Replace with '!!' operator"
// WITH_RUNTIME

fun foo(p: Array<String?>) {
    val v = p[0]
    // now let's check it for null
    <caret>assert(v != null /* null */) // 'v' should not be null
}