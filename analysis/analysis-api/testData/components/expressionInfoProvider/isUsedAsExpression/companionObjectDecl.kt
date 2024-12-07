interface I<T> {
    fun i(t: T): Int
}

class C<T>(val x: Int): I<T> {
    <expr>companion object {
        val K: Int = 58
    }</expr>

    fun test(): Int {
        return 45 * K
    }

    fun count(xs: List<T>): Int {
        return xs.size
    }

    override fun i(t: T): Int {
        return test() + t.hashCode()
    }

    inner class B() {

    }
}