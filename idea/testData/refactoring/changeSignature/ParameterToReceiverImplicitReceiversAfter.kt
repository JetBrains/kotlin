public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class C {
    fun String.foo() {
        with(1) {
            bar()
        }
    }

    fun Int.bar() {}
}