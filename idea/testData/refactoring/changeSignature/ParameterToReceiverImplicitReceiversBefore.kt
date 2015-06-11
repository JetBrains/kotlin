public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class C {
    fun <caret>foo(s: String) {
        with(1) {
            bar()
        }
    }

    fun Int.bar() {}
}