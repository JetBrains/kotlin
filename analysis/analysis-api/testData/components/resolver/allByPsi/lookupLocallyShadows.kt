// WITH_STDLIB

fun shadowing(x: Int) {
    fun g(x: Int) {
        return x
    }
    {x: Int -> x}
    x + 1
    class A(val x: Int) {
        val v: Int = x
            get() = x
        inner class B(x: Int) {
            val x: Int = x
                get() = x
            val v: Int = x
                get() = x
        }
    }
    return
}

fun selfReferential(x: Int) {
    val x = x
    val (x, y) = x to 1
    for (x in x..y) {
    }
}
