// FIR_IDENTICAL
class Test<E>(var c: Collection<E>) {
    fun test() {
        if (c is List<*>) {
            c.size // smartcast is unstable, but `size` is also available from `Collection`, so there should not be any error
        }
    }
}
