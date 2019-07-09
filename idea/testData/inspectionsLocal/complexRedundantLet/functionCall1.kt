// WITH_RUNTIME

fun foo(s: String, i: Int) = s.length + i

fun test() {
    val s = ""
    s.let<caret> { foo("", 1) }
}