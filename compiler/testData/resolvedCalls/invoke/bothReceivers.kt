class Foo() {
    fun Int.invoke() {}
}

fun bar(f: Foo) {
    1.f<caret>()
}
