// FIR_IDENTICAL
object A {
    operator fun get(vararg va: Int): Int = 10

    operator fun set(vararg va: Int, value: Int) {}
}

fun test() {
    A[1, 2, 3]++
}
