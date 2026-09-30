val x = object<T> {
    fun foo(t: T) {}
}

fun <R> test() {
    val y = object<T : R, K : Comparable<K>> {
        fun foo(t: T): K = null!!
    }
}
