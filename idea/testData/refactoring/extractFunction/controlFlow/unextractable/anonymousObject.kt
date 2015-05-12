interface Callable<T> {
    fun call(): T
}

fun foo(a: Int): Int {
    // SIBLING:
    val o = <selection>object: Callable<Int> {
        val b: Int = 1

        override fun call(): Int {
            return a + b
        }
    }</selection>
    return o.call()
}
