val x = object<T> {
    fun foo(t: T) {}
}

fun <R> test() {
    val y = object<T : R> {
        fun foo(t: T) {}
    }
}
