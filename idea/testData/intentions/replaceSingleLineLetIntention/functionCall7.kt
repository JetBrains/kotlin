// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo(s: String, i: Int) = s.length + i

fun test() {
    val s = ""
    s.let<caret> { foo(it, it.length) }
}