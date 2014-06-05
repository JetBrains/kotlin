class Foo() {
    fun Int.invoke() {}
}

fun bar(f: Foo, i: Int) {
    with (i) {
        f<caret>()
    }
}

fun <T, R> with(receiver: T, f: T.() -> R) : R = throw Exception()