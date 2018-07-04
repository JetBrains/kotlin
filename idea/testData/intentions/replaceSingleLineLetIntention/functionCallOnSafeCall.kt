// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo(s: String, i: Int) = s.length + i

fun test() {
    val nullable: String? = null
    nullable?.let<caret> { foo(it, 1) }
}