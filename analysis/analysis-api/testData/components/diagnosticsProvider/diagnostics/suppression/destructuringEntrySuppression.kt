fun d() {
    val (a, @Suppress("UNUSED_VARIABLE") b) = A(1, 2)
}

data class A(val i: Int, val b: Int)
