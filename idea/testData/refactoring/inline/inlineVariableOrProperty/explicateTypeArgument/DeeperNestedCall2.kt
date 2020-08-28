fun foo(f: List<Int>) {}

fun f() {
    val v : List<Int> = ArrayList(listOf())
    foo(<caret>v)
}
