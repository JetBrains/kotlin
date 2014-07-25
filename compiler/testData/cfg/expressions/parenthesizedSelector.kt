fun <T> with(t: T, f :  T.() -> Unit) {
    t.f()
}

val Int.foo: String.() -> Unit get() = {}
fun bar() {
    with(1) {
        "".(foo)()
    }
}