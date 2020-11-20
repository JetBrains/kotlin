// "Add non-null asserted (!!) call" "true"
class Foo {
    fun f() = 1
}

fun Foo?.test() {
    val f = ::f<caret>
}