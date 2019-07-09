// WITH_RUNTIME

fun foo(s: String, i: Int) = s.length + i

fun test() {
    fun bar() = ""
    bar().let<caret> { foo(it, 1) }
}