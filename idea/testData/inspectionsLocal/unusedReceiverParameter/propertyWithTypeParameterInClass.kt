class Test<T> {
    val <caret>T.bar: Int
        get() = 1

    fun test(t: T) {
        t.bar
    }
}