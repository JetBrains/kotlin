class Inv<T> {
    fun unwrap(): T = null!!
}

fun captured(inv: Inv<out Number>) {
    val capturedResult = inv.unwrap()
}

fun intersection(x: Any) {
    if (x is Comparable<*> && x is Number) {
        val intersectionResult = x
    }
}
