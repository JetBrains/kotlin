class Foo() {
    fun Int.invoke() {}
}

fun bar(f: Foo, i: Int) {
    with (i) {
        f<caret>()
    }
}