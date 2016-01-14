// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo(p: Array<String?>) {
    val v = p[0]
    <caret>assert(v != null, { val t = 1; "Should be not null: $t" })
}